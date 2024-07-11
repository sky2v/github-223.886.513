// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.ui.toolwindow.create

import git4idea.remote.hosting.knownRepositories
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.util.EventDispatcher
import git4idea.GitBranch
import git4idea.GitRemoteBranch
import git4idea.ui.branch.MergeDirectionModel
import cn.osc.gitee.util.GEGitRepositoryMapping
import cn.osc.gitee.util.GEHostedRepositoriesManager
import cn.osc.gitee.util.GiteeUtil.Delegates.observableField

class GEPRMergeDirectionModelImpl(override val baseRepo: GEGitRepositoryMapping,
                                  private val repositoriesManager: GEHostedRepositoriesManager) : MergeDirectionModel<GEGitRepositoryMapping> {

  private val changeEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

  override var baseBranch: GitRemoteBranch? by observableField(null, changeEventDispatcher)
  override var headRepo: GEGitRepositoryMapping? = null
    private set
  override var headBranch: GitBranch? = null
    private set
  override var headSetByUser: Boolean = false

  override fun setHead(repo: GEGitRepositoryMapping?, branch: GitBranch?) {
    headRepo = repo
    headBranch = branch
    changeEventDispatcher.multicaster.eventOccurred()
  }

  override fun addAndInvokeDirectionChangesListener(listener: () -> Unit) =
    SimpleEventListener.addAndInvokeListener(changeEventDispatcher, listener)

  override fun getKnownRepoMappings(): List<GEGitRepositoryMapping> = repositoriesManager.knownRepositories.toList()
}