// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.data.service

import com.intellij.openapi.Disposable
import com.intellij.util.concurrency.annotations.RequiresEdt
import cn.osc.gitee.github.api.GHRepositoryCoordinates
import cn.osc.gitee.github.api.data.GHLabel
import cn.osc.gitee.github.api.data.GHUser
import cn.osc.gitee.github.api.data.pullrequest.GHPullRequestRequestedReviewer
import cn.osc.gitee.github.api.data.pullrequest.GHTeam
import cn.osc.gitee.github.util.GHGitRepositoryMapping
import git4idea.remote.GitRemoteUrlCoordinates
import java.util.concurrent.CompletableFuture

interface GHPRRepositoryDataService : Disposable {
  val remoteCoordinates: GitRemoteUrlCoordinates
  val repositoryCoordinates: GHRepositoryCoordinates
  val repositoryMapping: GHGitRepositoryMapping
    get() = GHGitRepositoryMapping(repositoryCoordinates, remoteCoordinates)

  val repositoryId: String
  val defaultBranchName: String?
  val isFork: Boolean

  val collaborators: CompletableFuture<List<GHUser>>
  val teams: CompletableFuture<List<GHTeam>>
  val potentialReviewers: CompletableFuture<List<GHPullRequestRequestedReviewer>>
  val issuesAssignees: CompletableFuture<List<GHUser>>
  val labels: CompletableFuture<List<GHLabel>>

  @RequiresEdt
  fun resetData()
}