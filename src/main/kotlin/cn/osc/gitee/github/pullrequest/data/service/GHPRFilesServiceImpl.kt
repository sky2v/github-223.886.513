// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.data.service

import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import cn.osc.gitee.github.api.GHGQLRequests
import cn.osc.gitee.github.api.GHRepositoryCoordinates
import cn.osc.gitee.github.api.GithubApiRequestExecutor
import cn.osc.gitee.github.api.data.pullrequest.GHPullRequestChangedFile
import cn.osc.gitee.github.api.util.SimpleGHGQLPagesLoader
import cn.osc.gitee.github.pullrequest.data.GHPRIdentifier
import cn.osc.gitee.github.pullrequest.data.service.GHServiceUtil.logError
import java.util.concurrent.CompletableFuture

private val LOG = logger<GHPRFilesServiceImpl>()

class GHPRFilesServiceImpl(
  private val progressManager: ProgressManager,
  private val requestExecutor: GithubApiRequestExecutor,
  private val repository: GHRepositoryCoordinates
) : GHPRFilesService {

  override fun loadFiles(
    progressIndicator: ProgressIndicator,
    pullRequestId: GHPRIdentifier
  ): CompletableFuture<List<GHPullRequestChangedFile>> =
    progressManager
      .submitIOTask(progressIndicator) { indicator ->
        val loader = SimpleGHGQLPagesLoader(
          requestExecutor,
          { GHGQLRequests.PullRequest.files(repository, pullRequestId.number, it) }
        )

        loader.loadAll(indicator)
      }
      .logError(LOG, "Error occurred while loading pull request files")

  override fun updateViewedState(
    progressIndicator: ProgressIndicator,
    pullRequestId: GHPRIdentifier,
    path: String,
    isViewed: Boolean
  ): CompletableFuture<Unit> =
    progressManager
      .submitIOTask(progressIndicator) { indicator ->
        val request =
          if (isViewed) GHGQLRequests.PullRequest.markFileAsViewed(repository.serverPath, pullRequestId.id, path)
          else GHGQLRequests.PullRequest.unmarkFileAsViewed(repository.serverPath, pullRequestId.id, path)

        requestExecutor.execute(indicator, request)
      }
      .logError(LOG, "Error occurred while updating file viewed state")
}