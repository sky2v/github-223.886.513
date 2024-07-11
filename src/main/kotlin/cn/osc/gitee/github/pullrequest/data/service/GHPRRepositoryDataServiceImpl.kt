// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.data.service

import com.intellij.openapi.progress.ProgressManager
import cn.osc.gitee.github.api.GHGQLRequests
import cn.osc.gitee.github.api.GHRepositoryCoordinates
import cn.osc.gitee.github.api.GithubApiRequestExecutor
import cn.osc.gitee.github.api.GithubApiRequests
import cn.osc.gitee.github.api.data.GHLabel
import cn.osc.gitee.github.api.data.GHRepositoryOwnerName
import cn.osc.gitee.github.api.data.GHUser
import cn.osc.gitee.github.api.data.GithubUserWithPermissions
import cn.osc.gitee.github.api.data.pullrequest.GHPullRequestRequestedReviewer
import cn.osc.gitee.github.api.data.pullrequest.GHTeam
import cn.osc.gitee.github.api.util.GithubApiPagesLoader
import cn.osc.gitee.github.api.util.SimpleGHGQLPagesLoader
import git4idea.remote.GitRemoteUrlCoordinates
import cn.osc.gitee.github.util.LazyCancellableBackgroundProcessValue
import java.util.concurrent.CompletableFuture
import java.util.function.BiFunction

class GHPRRepositoryDataServiceImpl internal constructor(progressManager: ProgressManager,
                                                         private val requestExecutor: GithubApiRequestExecutor,
                                                         override val remoteCoordinates: GitRemoteUrlCoordinates,
                                                         override val repositoryCoordinates: GHRepositoryCoordinates,
                                                         private val repoOwner: GHRepositoryOwnerName,
                                                         override val repositoryId: String,
                                                         override val defaultBranchName: String?,
                                                         override val isFork: Boolean)
  : GHPRRepositoryDataService {

  private val serverPath = repositoryCoordinates.serverPath
  private val repoPath = repositoryCoordinates.repositoryPath

  init {
    requestExecutor.addListener(this) {
      resetData()
    }
  }

  private val collaboratorsValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
    GithubApiPagesLoader
      .loadAll(requestExecutor, indicator,
               GithubApiRequests.Repos.Collaborators.pages(serverPath, repoPath.owner, repoPath.repository))
  }

  override val collaborators: CompletableFuture<List<GHUser>>
    get() = collaboratorsValue.value.thenApply { list ->
      list.map { GHUser(it.nodeId, it.login, it.htmlUrl, it.avatarUrl ?: "", null) }
    }

  private val teamsValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
    if (repoOwner !is GHRepositoryOwnerName.Organization) emptyList()
    else SimpleGHGQLPagesLoader(requestExecutor, {
      GHGQLRequests.Organization.Team.findAll(serverPath, repoOwner.login, it)
    }).loadAll(indicator)
  }

  override val teams: CompletableFuture<List<GHTeam>>
    get() = teamsValue.value

  override val potentialReviewers: CompletableFuture<List<GHPullRequestRequestedReviewer>>
    get() = collaboratorsValue.value.thenCombine(teams,
                                                 BiFunction<List<GithubUserWithPermissions>, List<GHTeam>, List<GHPullRequestRequestedReviewer>> { users, teams ->
                                                   users
                                                     .filter { it.permissions.isPush }
                                                     .map { GHUser(it.nodeId, it.login, it.htmlUrl, it.avatarUrl ?: "", null) } +
                                                   teams
                                                 })

  private val assigneesValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
    GithubApiPagesLoader
      .loadAll(requestExecutor, indicator,
               GithubApiRequests.Repos.Assignees.pages(serverPath, repoPath.owner, repoPath.repository))
      .map { GHUser(it.nodeId, it.login, it.htmlUrl, it.avatarUrl ?: "", null) }
  }

  override val issuesAssignees: CompletableFuture<List<GHUser>>
    get() = assigneesValue.value

  private val labelsValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
    GithubApiPagesLoader
      .loadAll(requestExecutor, indicator,
               GithubApiRequests.Repos.Labels.pages(serverPath, repoPath.owner, repoPath.repository))
      .map { GHLabel(it.nodeId, it.url, it.name, it.color) }
  }

  override val labels: CompletableFuture<List<GHLabel>>
    get() = labelsValue.value

  override fun resetData() {
    collaboratorsValue.drop()
    teamsValue.drop()
    assigneesValue.drop()
    labelsValue.drop()
  }

  override fun dispose() {
    resetData()
  }
}