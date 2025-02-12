// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.ui.changes

import com.intellij.collaboration.async.CompletableFutureUtil.handleOnEdt
import com.intellij.collaboration.ui.codereview.CodeReviewProgressTreeModel
import com.intellij.collaboration.ui.codereview.setupCodeReviewProgressModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangesUtil
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode
import com.intellij.openapi.vcs.changes.ui.ChangesTree
import com.intellij.vcsUtil.VcsFileUtil
import git4idea.repo.GitRepository
import cn.osc.gitee.api.data.pullrequest.GEPullRequestFileViewedState
import cn.osc.gitee.api.data.pullrequest.isViewed
import cn.osc.gitee.pullrequest.data.provider.GEPRReviewDataProvider
import cn.osc.gitee.pullrequest.data.provider.GEPRViewedStateDataProvider

internal fun ChangesTree.showPullRequestProgress(
  parent: Disposable,
  repository: GitRepository,
  reviewData: GEPRReviewDataProvider,
  viewedStateData: GEPRViewedStateDataProvider
) {
  val model = GEPRProgressTreeModel(repository, reviewData, viewedStateData)
  Disposer.register(parent, model)

  setupCodeReviewProgressModel(parent, model)
}

private class GEPRProgressTreeModel(
  private val repository: GitRepository,
  private val reviewData: GEPRReviewDataProvider,
  private val viewedStateData: GEPRViewedStateDataProvider
) : CodeReviewProgressTreeModel<Change>(),
    Disposable {

  private var unresolvedThreadsCount: Map<String, Int> = emptyMap()
  private var filesViewedState: Map<String, GEPullRequestFileViewedState> = emptyMap()

  init {
    loadThreads()
    reviewData.addReviewThreadsListener(this) { loadThreads() }

    loadViewedState()
    viewedStateData.addViewedStateListener(this) { loadViewedState() }
  }

  private fun loadThreads() {
    reviewData.loadReviewThreads().handleOnEdt(this) { threads, _ ->
      threads ?: return@handleOnEdt // error

      unresolvedThreadsCount = threads.filter { !it.isResolved }.groupingBy { it.path }.eachCount()
      fireModelChanged()
    }
  }

  private fun loadViewedState() {
    viewedStateData.loadViewedState().handleOnEdt(this) { viewedState, _ ->
      viewedState ?: return@handleOnEdt // error

      filesViewedState = viewedState
      fireModelChanged()
    }
  }

  override fun dispose() = Unit

  override fun asLeaf(node: ChangesBrowserNode<*>): Change? {
    return node.userObject as? Change
  }

  override fun isRead(leafValue: Change): Boolean {
    val repositoryRelativePath = VcsFileUtil.relativePath(repository.root, ChangesUtil.getFilePath(leafValue))

    val viewedState = filesViewedState[repositoryRelativePath] ?: return true
    return viewedState.isViewed()
  }

  override fun getUnresolvedDiscussionsCount(leafValue: Change): Int {
    val repositoryRelativePath = VcsFileUtil.relativePath(repository.root, ChangesUtil.getFilePath(leafValue))

    return unresolvedThreadsCount[repositoryRelativePath] ?: 0
  }
}