// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data.service

import com.intellij.openapi.progress.ProgressManager
import cn.osc.gitee.api.GEGQLRequests
import cn.osc.gitee.api.GERepositoryCoordinates
import cn.osc.gitee.api.GiteeApiRequestExecutor
import cn.osc.gitee.api.GiteeApiRequests
import cn.osc.gitee.api.data.GELabel
import cn.osc.gitee.api.data.GERepositoryOwnerName
import cn.osc.gitee.api.data.GEUser
import cn.osc.gitee.api.data.GiteeUserWithPermissions
import cn.osc.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import cn.osc.gitee.api.data.pullrequest.GETeam
import cn.osc.gitee.api.util.GiteeApiPagesLoader
import cn.osc.gitee.api.util.SimpleGHGQLPagesLoader
import git4idea.remote.GitRemoteUrlCoordinates
import cn.osc.gitee.util.LazyCancellableBackgroundProcessValue
import java.util.concurrent.CompletableFuture
import java.util.function.BiFunction

class GEPRRepositoryDataServiceImpl internal constructor(progressManager: ProgressManager,
                                                         private val requestExecutor: GiteeApiRequestExecutor,
                                                         override val remoteCoordinates: GitRemoteUrlCoordinates,
                                                         override val repositoryCoordinates: GERepositoryCoordinates,
                                                         private val repoOwner: GERepositoryOwnerName,
                                                         override val repositoryId: String,
                                                         override val defaultBranchName: String?,
                                                         override val isFork: Boolean)
  : GEPRRepositoryDataService {

  private val serverPath = repositoryCoordinates.serverPath
  private val repoPath = repositoryCoordinates.repositoryPath

  init {
    requestExecutor.addListener(this) {
      resetData()
    }
  }

  private val collaboratorsValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
    GiteeApiPagesLoader
      .loadAll(requestExecutor, indicator,
               GiteeApiRequests.Repos.Collaborators.pages(serverPath, repoPath.owner, repoPath.repository))
  }

  override val collaborators: CompletableFuture<List<GEUser>>
    get() = collaboratorsValue.value.thenApply { list ->
      list.map { GEUser(it.nodeId, it.login, it.htmlUrl, it.avatarUrl ?: "", null) }
    }

  private val teamsValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
    if (repoOwner !is GERepositoryOwnerName.Organization) emptyList()
    else SimpleGHGQLPagesLoader(requestExecutor, {
      GEGQLRequests.Organization.Team.findAll(serverPath, repoOwner.login, it)
    }).loadAll(indicator)
  }

  override val teams: CompletableFuture<List<GETeam>>
    get() = teamsValue.value

  override val potentialReviewers: CompletableFuture<List<GEPullRequestRequestedReviewer>>
    get() = collaboratorsValue.value.thenCombine(teams,
                                                 BiFunction<List<GiteeUserWithPermissions>, List<GETeam>, List<GEPullRequestRequestedReviewer>> { users, teams ->
                                                   users
                                                     .filter { it.permissions.isPush }
                                                     .map { GEUser(it.nodeId, it.login, it.htmlUrl, it.avatarUrl ?: "", null) } +
                                                   teams
                                                 })

  private val assigneesValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
    GiteeApiPagesLoader
      .loadAll(requestExecutor, indicator,
               GiteeApiRequests.Repos.Assignees.pages(serverPath, repoPath.owner, repoPath.repository))
      .map { GEUser(it.nodeId, it.login, it.htmlUrl, it.avatarUrl ?: "", null) }
  }

  override val issuesAssignees: CompletableFuture<List<GEUser>>
    get() = assigneesValue.value

  private val labelsValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
    GiteeApiPagesLoader
      .loadAll(requestExecutor, indicator,
               GiteeApiRequests.Repos.Labels.pages(serverPath, repoPath.owner, repoPath.repository))
      .map { GELabel(it.nodeId, it.url, it.name, it.color) }
  }

  override val labels: CompletableFuture<List<GELabel>>
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