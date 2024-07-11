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
import cn.osc.gitee.api.data.GELabel
import cn.osc.gitee.api.data.GEUser
import cn.osc.gitee.api.data.pullrequest.GEPullRequest
import cn.osc.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import cn.osc.gitee.api.data.pullrequest.GETeam
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.pullrequest.GENotFoundException
import cn.osc.gitee.pullrequest.data.GEPRIdentifier
import cn.osc.gitee.pullrequest.data.service.GEServiceUtil.logError
import com.intellij.collaboration.util.CollectionDelta
import java.util.concurrent.CompletableFuture

class GEPRDetailsServiceImpl(private val progressManager: ProgressManager,
                             private val requestExecutor: GiteeApiRequestExecutor,
                             private val repository: GERepositoryCoordinates) : GEPRDetailsService {

  private val serverPath = repository.serverPath
  private val repoPath = repository.repositoryPath

  override fun loadDetails(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier): CompletableFuture<GEPullRequest> =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(it, GEGQLRequests.PullRequest.findOne(repository, pullRequestId.number))
      ?: throw GENotFoundException("Pull request ${pullRequestId.number} does not exist")
    }.logError(LOG, "Error occurred while loading PR details")

  override fun updateDetails(indicator: ProgressIndicator, pullRequestId: GEPRIdentifier, title: String?, description: String?)
    : CompletableFuture<GEPullRequest> = progressManager.submitIOTask(indicator) {
    requestExecutor.execute(it, GEGQLRequests.PullRequest.update(repository, pullRequestId.id, title, description))
  }.logError(LOG, "Error occurred while loading PR details")

  override fun adjustReviewers(indicator: ProgressIndicator,
                               pullRequestId: GEPRIdentifier,
                               delta: CollectionDelta<GEPullRequestRequestedReviewer>) =
    progressManager.submitIOTask(indicator) {
      it.text = GiteeBundle.message("pull.request.details.adjusting.reviewers")
      val removedItems = delta.removedItems
      if (removedItems.isNotEmpty()) {
        it.text2 = GiteeBundle.message("pull.request.removing.reviewers")
        requestExecutor.execute(it,
                                GiteeApiRequests.Repos.PullRequests.Reviewers
                                  .remove(serverPath, repoPath.owner, repoPath.repository, pullRequestId.number,
                                          removedItems.filterIsInstance(GEUser::class.java).map { it.login },
                                          removedItems.filterIsInstance(GETeam::class.java).map { it.slug }))
      }
      val newItems = delta.newItems
      if (newItems.isNotEmpty()) {
        it.text2 = GiteeBundle.message("pull.request.adding.reviewers")
        requestExecutor.execute(it,
                                GiteeApiRequests.Repos.PullRequests.Reviewers
                                  .add(serverPath, repoPath.owner, repoPath.repository, pullRequestId.number,
                                       newItems.filterIsInstance(GEUser::class.java).map { it.login },
                                       newItems.filterIsInstance(GETeam::class.java).map { it.slug }))
      }
    }
      .logError(LOG, "Error occurred while adjusting the list of reviewers")

  override fun adjustAssignees(indicator: ProgressIndicator, pullRequestId: GEPRIdentifier, delta: CollectionDelta<GEUser>) =
    progressManager.submitIOTask(indicator) {
      it.text = GiteeBundle.message("pull.request.details.adjusting.assignees")
      requestExecutor.execute(it,
                              GiteeApiRequests.Repos.Issues.updateAssignees(serverPath, repoPath.owner, repoPath.repository,
                                                                             pullRequestId.number.toString(),
                                                                             delta.newCollection.map { it.login }))
      return@submitIOTask
    }
      .logError(LOG, "Error occurred while adjusting the list of assignees")

  override fun adjustLabels(indicator: ProgressIndicator, pullRequestId: GEPRIdentifier, delta: CollectionDelta<GELabel>) =
    progressManager.submitIOTask(indicator) {
      it.text = GiteeBundle.message("pull.request.details.adjusting.labels")
      requestExecutor.execute(indicator,
                              GiteeApiRequests.Repos.Issues.Labels
                                .replace(serverPath, repoPath.owner, repoPath.repository, pullRequestId.number.toString(),
                                         delta.newCollection.map { it.name }))
      return@submitIOTask
    }.logError(LOG, "Error occurred while adjusting the list of labels")

  companion object {
    private val LOG = logger<GEPRDetailsService>()
  }
}