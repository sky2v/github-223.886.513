// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data.service

import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import cn.osc.gitee.api.*
import cn.osc.gitee.api.data.GEBranchProtectionRules
import cn.osc.gitee.api.data.GERepositoryPermissionLevel
import cn.osc.gitee.api.data.GiteeIssueState
import cn.osc.gitee.api.data.GiteePullRequestMergeMethod
import cn.osc.gitee.pullrequest.GEPRStatisticsCollector
import cn.osc.gitee.pullrequest.data.GEPRIdentifier
import cn.osc.gitee.pullrequest.data.GEPRMergeabilityStateBuilder
import cn.osc.gitee.pullrequest.data.service.GEServiceUtil.logError
import java.util.concurrent.CompletableFuture

class GEPRStateServiceImpl internal constructor(private val progressManager: ProgressManager,
                                                private val securityService: GEPRSecurityService,
                                                private val requestExecutor: GiteeApiRequestExecutor,
                                                private val serverPath: GiteeServerPath,
                                                private val repoPath: GERepositoryPath)
  : GEPRStateService {

  private val repository = GERepositoryCoordinates(serverPath, repoPath)

  override fun loadBranchProtectionRules(progressIndicator: ProgressIndicator,
                                         pullRequestId: GEPRIdentifier,
                                         baseBranch: String): CompletableFuture<GEBranchProtectionRules?> {
    if (!securityService.currentUserHasPermissionLevel(GERepositoryPermissionLevel.WRITE)) return CompletableFuture.completedFuture(null)

    return progressManager.submitIOTask(progressIndicator) {
      try {
        requestExecutor.execute(it, GiteeApiRequests.Repos.Branches.getProtection(repository, baseBranch))
      }
      catch (e: Exception) {
        // assume there are no restrictions
        if (e !is ProcessCanceledException) LOG.info("Error occurred while loading branch protection rules for $baseBranch", e)
        null
      }
    }
  }

  override fun loadMergeabilityState(progressIndicator: ProgressIndicator,
                                     pullRequestId: GEPRIdentifier,
                                     headRefOid: String,
                                     prHtmlUrl: String,
                                     baseBranchProtectionRules: GEBranchProtectionRules?) =
    progressManager.submitIOTask(progressIndicator) {
      val mergeabilityData = requestExecutor.execute(it, GEGQLRequests.PullRequest.mergeabilityData(repository, pullRequestId.number))
                             ?: error("Could not find pull request $pullRequestId.number")
      val builder = GEPRMergeabilityStateBuilder(headRefOid, prHtmlUrl,
                                                 mergeabilityData)
      if (baseBranchProtectionRules != null) {
        builder.withRestrictions(securityService, baseBranchProtectionRules)
      }
      builder.build()
    }.logError(LOG, "Error occurred while loading mergeability state data for PR ${pullRequestId.number}")


  override fun close(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier) =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(it,
                              GiteeApiRequests.Repos.PullRequests.update(serverPath, repoPath.owner, repoPath.repository,
                                                                          pullRequestId.number,
                                                                          state = GiteeIssueState.closed))
      return@submitIOTask
    }.logError(LOG, "Error occurred while closing PR ${pullRequestId.number}")

  override fun reopen(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier) =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(it,
                              GiteeApiRequests.Repos.PullRequests.update(serverPath, repoPath.owner, repoPath.repository,
                                                                          pullRequestId.number,
                                                                          state = GiteeIssueState.open))
      return@submitIOTask
    }.logError(LOG, "Error occurred while reopening PR ${pullRequestId.number}")

  override fun markReadyForReview(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier) =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(it,
                              GEGQLRequests.PullRequest.markReadyForReview(repository, pullRequestId.id))
      return@submitIOTask
    }.logError(LOG, "Error occurred while marking PR ${pullRequestId.number} ready fro review")

  override fun merge(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier,
                     commitMessage: Pair<String, String>, currentHeadRef: String) =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(it, GiteeApiRequests.Repos.PullRequests.merge(serverPath, repoPath, pullRequestId.number,
                                                                             commitMessage.first, commitMessage.second,
                                                                             currentHeadRef))
      GEPRStatisticsCollector.logMergedEvent(GiteePullRequestMergeMethod.merge)
      return@submitIOTask
    }.logError(LOG, "Error occurred while merging PR ${pullRequestId.number}")


  override fun rebaseMerge(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier,
                           currentHeadRef: String) =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(it,
                              GiteeApiRequests.Repos.PullRequests.rebaseMerge(serverPath, repoPath, pullRequestId.number,
                                                                               currentHeadRef))
      GEPRStatisticsCollector.logMergedEvent(GiteePullRequestMergeMethod.rebase)
      return@submitIOTask
    }.logError(LOG, "Error occurred while rebasing PR ${pullRequestId.number}")

  override fun squashMerge(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier,
                           commitMessage: Pair<String, String>, currentHeadRef: String) =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(it,
                              GiteeApiRequests.Repos.PullRequests.squashMerge(serverPath, repoPath, pullRequestId.number,
                                                                               commitMessage.first, commitMessage.second,
                                                                               currentHeadRef))
      GEPRStatisticsCollector.logMergedEvent(GiteePullRequestMergeMethod.squash)
      return@submitIOTask
    }.logError(LOG, "Error occurred while squash-merging PR ${pullRequestId.number}")

  companion object {
    private val LOG = logger<GEPRStateService>()
  }
}