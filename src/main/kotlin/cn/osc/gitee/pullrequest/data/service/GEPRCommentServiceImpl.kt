// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data.service

import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import cn.osc.gitee.api.GEGQLRequests
import cn.osc.gitee.api.GERepositoryCoordinates
import cn.osc.gitee.api.GiteeApiRequestExecutor
import cn.osc.gitee.api.GiteeApiRequests
import cn.osc.gitee.api.data.GiteeIssueCommentWithHtml
import cn.osc.gitee.pullrequest.data.GEPRIdentifier
import cn.osc.gitee.pullrequest.data.service.GEServiceUtil.logError
import java.util.concurrent.CompletableFuture

class GEPRCommentServiceImpl(private val progressManager: ProgressManager,
                             private val requestExecutor: GiteeApiRequestExecutor,
                             private val repository: GERepositoryCoordinates) : GEPRCommentService {

  override fun addComment(progressIndicator: ProgressIndicator,
                          pullRequestId: GEPRIdentifier,
                          body: String): CompletableFuture<GiteeIssueCommentWithHtml> {
    return progressManager.submitIOTask(progressIndicator) {
      val comment = requestExecutor.execute(
        it,
        GiteeApiRequests.Repos.Issues.Comments.create(repository, pullRequestId.number, body))
      comment
    }.logError(LOG, "Error occurred while adding PR comment")
  }

  override fun updateComment(progressIndicator: ProgressIndicator, commentId: String, text: String) =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(it, GEGQLRequests.Comment.updateComment(repository.serverPath, commentId, text))
    }.logError(LOG, "Error occurred while updating comment")

  override fun deleteComment(progressIndicator: ProgressIndicator, commentId: String) =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(it, GEGQLRequests.Comment.deleteComment(repository.serverPath, commentId))
    }.logError(LOG, "Error occurred while deleting comment")

  companion object {
    private val LOG = logger<GEPRCommentService>()
  }
}