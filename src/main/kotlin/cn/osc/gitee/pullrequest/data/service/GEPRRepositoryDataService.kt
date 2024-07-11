// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data.service

import com.intellij.openapi.Disposable
import com.intellij.util.concurrency.annotations.RequiresEdt
import cn.osc.gitee.api.GERepositoryCoordinates
import cn.osc.gitee.api.data.GELabel
import cn.osc.gitee.api.data.GEUser
import cn.osc.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import cn.osc.gitee.api.data.pullrequest.GETeam
import cn.osc.gitee.util.GEGitRepositoryMapping
import git4idea.remote.GitRemoteUrlCoordinates
import java.util.concurrent.CompletableFuture

interface GEPRRepositoryDataService : Disposable {
  val remoteCoordinates: GitRemoteUrlCoordinates
  val repositoryCoordinates: GERepositoryCoordinates
  val repositoryMapping: GEGitRepositoryMapping
    get() = GEGitRepositoryMapping(repositoryCoordinates, remoteCoordinates)

  val repositoryId: String
  val defaultBranchName: String?
  val isFork: Boolean

  val collaborators: CompletableFuture<List<GEUser>>
  val teams: CompletableFuture<List<GETeam>>
  val potentialReviewers: CompletableFuture<List<GEPullRequestRequestedReviewer>>
  val issuesAssignees: CompletableFuture<List<GEUser>>
  val labels: CompletableFuture<List<GELabel>>

  @RequiresEdt
  fun resetData()
}