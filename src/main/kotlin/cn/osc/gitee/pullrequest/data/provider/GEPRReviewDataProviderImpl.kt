// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data.provider

import com.intellij.collaboration.async.CompletableFutureUtil.completionOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.handleOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.diff.util.Side
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.messages.MessageBus
import cn.osc.gitee.api.data.GENode
import cn.osc.gitee.api.data.GENodes
import cn.osc.gitee.api.data.GEPullRequestReviewEvent
import cn.osc.gitee.api.data.pullrequest.GEPullRequestPendingReview
import cn.osc.gitee.api.data.pullrequest.GEPullRequestReviewComment
import cn.osc.gitee.api.data.pullrequest.GEPullRequestReviewState
import cn.osc.gitee.api.data.pullrequest.GEPullRequestReviewThread
import cn.osc.gitee.api.data.request.GEPullRequestDraftReviewComment
import cn.osc.gitee.api.data.request.GEPullRequestDraftReviewThread
import cn.osc.gitee.pullrequest.data.GEPRIdentifier
import cn.osc.gitee.pullrequest.data.service.GEPRReviewService
import cn.osc.gitee.util.LazyCancellableBackgroundProcessValue
import java.util.concurrent.CompletableFuture

class GEPRReviewDataProviderImpl(private val reviewService: GEPRReviewService,
                                 private val pullRequestId: GEPRIdentifier,
                                 private val messageBus: MessageBus)
  : GEPRReviewDataProvider, Disposable {

  override val submitReviewCommentDocument by lazy(LazyThreadSafetyMode.NONE) { EditorFactory.getInstance().createDocument("") }

  private val pendingReviewRequestValue = LazyCancellableBackgroundProcessValue.create {
    reviewService.loadPendingReview(it, pullRequestId)
  }

  private val reviewThreadsRequestValue = LazyCancellableBackgroundProcessValue.create {
    reviewService.loadReviewThreads(it, pullRequestId)
  }

  override fun loadPendingReview() = pendingReviewRequestValue.value

  override fun resetPendingReview() = pendingReviewRequestValue.drop()

  override fun loadReviewThreads() = reviewThreadsRequestValue.value

  override fun resetReviewThreads() = reviewThreadsRequestValue.drop()

  override fun createReview(progressIndicator: ProgressIndicator,
                            event: GEPullRequestReviewEvent?,
                            body: String?,
                            commitSha: String?,
                            comments: List<GEPullRequestDraftReviewComment>?,
                            threads: List<GEPullRequestDraftReviewThread>?): CompletableFuture<GEPullRequestPendingReview> {
    val future = reviewService.createReview(progressIndicator, pullRequestId, event, body, commitSha, comments, threads).notifyReviews()
    if (event == null) {
      pendingReviewRequestValue.overrideProcess(future.successOnEdt { it })
    }
    return if (comments.isNullOrEmpty() && threads.isNullOrEmpty()) future else future.dropReviews()
  }

  override fun submitReview(progressIndicator: ProgressIndicator,
                            reviewId: String,
                            event: GEPullRequestReviewEvent,
                            body: String?): CompletableFuture<out Any?> {
    val future = reviewService.submitReview(progressIndicator, pullRequestId, reviewId, event, body)
    pendingReviewRequestValue.overrideProcess(future.successOnEdt { null })
    return future.dropReviews().notifyReviews()
  }

  override fun updateReviewBody(progressIndicator: ProgressIndicator, reviewId: String, newText: String): CompletableFuture<String> =
    reviewService.updateReviewBody(progressIndicator, reviewId, newText).successOnEdt {
      messageBus.syncPublisher(GEPRDataOperationsListener.TOPIC).onReviewUpdated(reviewId, newText)
      it.body
    }

  override fun deleteReview(progressIndicator: ProgressIndicator, reviewId: String): CompletableFuture<out Any?> {
    val future = reviewService.deleteReview(progressIndicator, pullRequestId, reviewId)
    pendingReviewRequestValue.combineResult(future) { pendingReview, _ ->
      if (pendingReview != null && pendingReview.id == reviewId) throw ProcessCanceledException()
      else pendingReview
    }
    return future.dropReviews().notifyReviews()
  }

  override fun canComment() = reviewService.canComment()

  override fun addComment(progressIndicator: ProgressIndicator,
                          reviewId: String,
                          body: String,
                          commitSha: String,
                          fileName: String,
                          diffLine: Int): CompletableFuture<out GEPullRequestReviewComment> {
    val future =
      reviewService.addComment(progressIndicator, reviewId, body, commitSha, fileName, diffLine)

    pendingReviewRequestValue.overrideProcess(future.successOnEdt { it.pullRequestReview })
    return future.dropReviews().notifyReviews()
  }

  override fun addComment(progressIndicator: ProgressIndicator,
                          replyToCommentId: String,
                          body: String): CompletableFuture<out GEPullRequestReviewComment> {
    return pendingReviewRequestValue.value.thenCompose {
      val reviewId = it?.id
      if (reviewId == null) {
        createReview(progressIndicator).thenCompose { review ->
          reviewService.addComment(progressIndicator, pullRequestId, review.id, replyToCommentId, body).thenCompose { comment ->
            submitReview(progressIndicator, review.id, GEPullRequestReviewEvent.COMMENT, null).thenApply {
              comment
            }
          }.dropReviews().notifyReviews()
        }
      }
      else {
        val future = reviewService.addComment(progressIndicator, pullRequestId, reviewId, replyToCommentId, body)
        pendingReviewRequestValue.overrideProcess(future.successOnEdt { it.pullRequestReview })
        future.dropReviews().notifyReviews()
      }
    }
  }

  override fun deleteComment(progressIndicator: ProgressIndicator, commentId: String): CompletableFuture<out Any> {
    val future = reviewService.deleteComment(progressIndicator, pullRequestId, commentId)

    pendingReviewRequestValue.overrideProcess(future.handleOnEdt { result, error ->
      if (error != null || (result?.state != GEPullRequestReviewState.PENDING || result.comments.totalCount != 0)) {
        messageBus.syncPublisher(GEPRDataOperationsListener.TOPIC).onReviewsChanged()
        throw ProcessCanceledException()
      }
      null
    })
    reviewThreadsRequestValue.combineResult(future) { list, _ ->
      list.mapNotNull {
        val comments = it.comments.filter { comment -> comment.id != commentId }
        if (comments.isEmpty())
          null
        else
          GEPullRequestReviewThread(it.id, it.isResolved, it.isOutdated, it.path, it.side, it.line, it.startLine, GENodes(comments))
      }
    }

    return future
  }

  override fun updateComment(progressIndicator: ProgressIndicator, commentId: String, newText: String)
    : CompletableFuture<GEPullRequestReviewComment> {
    val future = reviewService.updateComment(progressIndicator, pullRequestId, commentId, newText)
    reviewThreadsRequestValue.combineResult(future) { list, newComment ->
      list.map {
        GEPullRequestReviewThread(it.id, it.isResolved, it.isOutdated, it.path, it.side, it.line, it.startLine,
                                  GENodes(it.comments.map { comment ->
                                    if (comment.id == commentId)
                                      GEPullRequestReviewComment(comment.id, comment.databaseId, comment.url, comment.author,
                                                                 newComment.body, comment.createdAt,
                                                                 comment.state, comment.commit,
                                                                 comment.originalCommit, comment.replyTo,
                                                                 comment.diffHunk,
                                                                 comment.reviewId?.let { GENode(it) }, comment.viewerCanDelete,
                                                                 comment.viewerCanUpdate)
                                    else comment
                                  }))
      }
    }
    return future
  }

  override fun createThread(progressIndicator: ProgressIndicator,
                            reviewId: String?, body: String, line: Int, side: Side, startLine: Int, fileName: String)
    : CompletableFuture<GEPullRequestReviewThread> {

    return if (reviewId == null) {
      createReview(progressIndicator).thenCompose { review ->
        reviewService.addThread(progressIndicator, review.id, body, line, side, startLine, fileName).thenCompose { thread ->
          submitReview(progressIndicator, review.id, GEPullRequestReviewEvent.COMMENT, null).thenApply {
            thread
          }
        }
      }
    }
    else {
      reviewService.addThread(progressIndicator, reviewId, body, line, side, startLine, fileName)
    }.dropReviews().notifyReviews()
  }

  override fun resolveThread(progressIndicator: ProgressIndicator, id: String): CompletableFuture<GEPullRequestReviewThread> {
    val future = reviewService.resolveThread(progressIndicator, pullRequestId, id)
    reviewThreadsRequestValue.combineResult(future) { list, thread ->
      list.map { if (it == thread) thread else it }
    }
    return future
  }

  override fun unresolveThread(progressIndicator: ProgressIndicator, id: String): CompletableFuture<GEPullRequestReviewThread> {
    val future = reviewService.unresolveThread(progressIndicator, pullRequestId, id)
    reviewThreadsRequestValue.combineResult(future) { list, thread ->
      list.map { if (it == thread) thread else it }
    }
    return future
  }

  private fun <T> CompletableFuture<T>.notifyReviews(): CompletableFuture<T> =
    completionOnEdt {
      messageBus.syncPublisher(GEPRDataOperationsListener.TOPIC).onReviewsChanged()
    }

  private fun <T> CompletableFuture<T>.dropReviews(): CompletableFuture<T> =
    completionOnEdt {
      reviewThreadsRequestValue.drop()
    }

  override fun addReviewThreadsListener(disposable: Disposable, listener: () -> Unit) =
    reviewThreadsRequestValue.addDropEventListener(disposable, listener)

  override fun addPendingReviewListener(disposable: Disposable, listener: () -> Unit) =
    pendingReviewRequestValue.addDropEventListener(disposable, listener)

  override fun dispose() {
    pendingReviewRequestValue.drop()
    reviewThreadsRequestValue.drop()
  }
}