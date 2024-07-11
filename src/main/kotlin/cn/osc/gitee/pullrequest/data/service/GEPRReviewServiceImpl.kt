// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data.service

import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.diff.util.Side
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import cn.osc.gitee.api.GEGQLRequests
import cn.osc.gitee.api.GERepositoryCoordinates
import cn.osc.gitee.api.GiteeApiRequestExecutor
import cn.osc.gitee.api.data.GEPullRequestReviewEvent
import cn.osc.gitee.api.data.GERepositoryPermissionLevel
import cn.osc.gitee.api.data.pullrequest.GEPullRequestPendingReview
import cn.osc.gitee.api.data.pullrequest.GEPullRequestReview
import cn.osc.gitee.api.data.pullrequest.GEPullRequestReviewCommentWithPendingReview
import cn.osc.gitee.api.data.pullrequest.GEPullRequestReviewThread
import cn.osc.gitee.api.data.request.GEPullRequestDraftReviewComment
import cn.osc.gitee.api.data.request.GEPullRequestDraftReviewThread
import cn.osc.gitee.api.util.SimpleGHGQLPagesLoader
import cn.osc.gitee.pullrequest.data.GEPRIdentifier
import cn.osc.gitee.pullrequest.data.service.GEServiceUtil.logError
import java.util.concurrent.CompletableFuture

class GEPRReviewServiceImpl(private val progressManager: ProgressManager,
                            private val securityService: GEPRSecurityService,
                            private val requestExecutor: GiteeApiRequestExecutor,
                            private val repository: GERepositoryCoordinates) : GEPRReviewService {

  override fun canComment() = securityService.currentUserHasPermissionLevel(GERepositoryPermissionLevel.READ)

  override fun loadPendingReview(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier) =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(progressIndicator,
                              GEGQLRequests.PullRequest.Review.pendingReviews(repository.serverPath, pullRequestId.id)).nodes.singleOrNull()
    }.logError(LOG, "Error occurred while loading pending review")

  override fun loadReviewThreads(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier) =
    progressManager.submitIOTask(progressIndicator) {
      SimpleGHGQLPagesLoader(requestExecutor, { p ->
        GEGQLRequests.PullRequest.reviewThreads(repository, pullRequestId.number, p)
      }).loadAll(it)
    }.logError(LOG, "Error occurred while loading review threads")

  override fun createReview(progressIndicator: ProgressIndicator,
                            pullRequestId: GEPRIdentifier,
                            event: GEPullRequestReviewEvent?,
                            body: String?,
                            commitSha: String?,
                            comments: List<GEPullRequestDraftReviewComment>?,
                            threads: List<GEPullRequestDraftReviewThread>?): CompletableFuture<GEPullRequestPendingReview> =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(progressIndicator,
                              GEGQLRequests.PullRequest.Review.create(repository.serverPath, pullRequestId.id, event, body,
                                                                      commitSha, comments, threads))
    }.logError(LOG, "Error occurred while creating review")

  override fun submitReview(progressIndicator: ProgressIndicator,
                            pullRequestId: GEPRIdentifier,
                            reviewId: String,
                            event: GEPullRequestReviewEvent,
                            body: String?): CompletableFuture<out Any?> =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(progressIndicator,
                              GEGQLRequests.PullRequest.Review.submit(repository.serverPath, reviewId, event, body))
    }.logError(LOG, "Error occurred while submitting review")

  override fun updateReviewBody(progressIndicator: ProgressIndicator,
                                reviewId: String,
                                newText: String): CompletableFuture<GEPullRequestReview> =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(progressIndicator,
                              GEGQLRequests.PullRequest.Review.updateBody(repository.serverPath, reviewId, newText))
    }.logError(LOG, "Error occurred while updating review")

  override fun deleteReview(progressIndicator: ProgressIndicator,
                            pullRequestId: GEPRIdentifier,
                            reviewId: String): CompletableFuture<out Any?> =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(progressIndicator,
                              GEGQLRequests.PullRequest.Review.delete(repository.serverPath, reviewId))
    }.logError(LOG, "Error occurred while deleting review")

  override fun addComment(progressIndicator: ProgressIndicator,
                          pullRequestId: GEPRIdentifier,
                          reviewId: String,
                          replyToCommentId: String,
                          body: String): CompletableFuture<GEPullRequestReviewCommentWithPendingReview> =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(
        it,
        GEGQLRequests.PullRequest.Review.addComment(repository.serverPath,
                                                    reviewId,
                                                    replyToCommentId,
                                                    body))
    }.logError(LOG, "Error occurred while adding review thread reply")

  override fun addComment(progressIndicator: ProgressIndicator, reviewId: String,
                          body: String, commitSha: String, fileName: String, diffLine: Int)
    : CompletableFuture<GEPullRequestReviewCommentWithPendingReview> =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(progressIndicator,
                              GEGQLRequests.PullRequest.Review.addComment(repository.serverPath,
                                                                          reviewId,
                                                                          body, commitSha, fileName,
                                                                          diffLine))
    }.logError(LOG, "Error occurred while adding review comment")

  override fun deleteComment(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier, commentId: String) =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(it, GEGQLRequests.PullRequest.Review.deleteComment(repository.serverPath, commentId))
    }.logError(LOG, "Error occurred while deleting review comment")

  override fun updateComment(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier, commentId: String, newText: String) =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(it, GEGQLRequests.PullRequest.Review.updateComment(repository.serverPath, commentId, newText))
    }.logError(LOG, "Error occurred while updating review comment")

  override fun addThread(progressIndicator: ProgressIndicator, reviewId: String, body: String,
                         line: Int, side: Side, startLine: Int, fileName: String): CompletableFuture<GEPullRequestReviewThread> =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(it, GEGQLRequests.PullRequest.Review.addThread(repository.serverPath, reviewId, body, line, side, startLine, fileName))
    }.logError(LOG, "Error occurred while adding review thread")

  override fun resolveThread(progressIndicator: ProgressIndicator,
                             pullRequestId: GEPRIdentifier,
                             id: String): CompletableFuture<GEPullRequestReviewThread> =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(it, GEGQLRequests.PullRequest.Review.resolveThread(repository.serverPath, id))
    }.logError(LOG, "Error occurred while resolving review thread")

  override fun unresolveThread(progressIndicator: ProgressIndicator,
                               pullRequestId: GEPRIdentifier,
                               id: String): CompletableFuture<GEPullRequestReviewThread> =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(it, GEGQLRequests.PullRequest.Review.unresolveThread(repository.serverPath, id))
    }.logError(LOG, "Error occurred while unresolving review thread")

  companion object {
    private val LOG = logger<GEPRReviewService>()
  }
}