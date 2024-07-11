// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import git4idea.branch.GitBranchUtil
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.util.GiteeGitHelper

class GEPRUpdateBranchAction : DumbAwareAction(GiteeBundle.messagePointer("pull.request.branch.update.action"),
                                               GiteeBundle.messagePointer("pull.request.branch.update.action.description"),
                                               null) {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  override fun update(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val repository = e.getData(GEPRActionKeys.GIT_REPOSITORY)
    val selection = e.getData(GEPRActionKeys.PULL_REQUEST_DATA_PROVIDER)
    val loadedDetails = selection?.detailsData?.loadedDetails
    val headRefName = loadedDetails?.headRefName
    val httpUrl = loadedDetails?.headRepository?.url
    val sshUrl = loadedDetails?.headRepository?.sshUrl
    val isFork = loadedDetails?.headRepository?.isFork ?: false

    e.presentation.isEnabled = project != null &&
                               !project.isDefault &&
                               selection != null &&
                               repository != null &&
                               GiteeGitHelper.getInstance().findRemote(repository, httpUrl, sshUrl)?.let { remote ->
                                 GiteeGitHelper.getInstance().findLocalBranch(repository, remote, isFork, headRefName) != null
                               } ?: false
  }

  override fun actionPerformed(e: AnActionEvent) {
    val repository = e.getRequiredData(GEPRActionKeys.GIT_REPOSITORY)
    val project = repository.project
    val loadedDetails = e.getRequiredData(GEPRActionKeys.PULL_REQUEST_DATA_PROVIDER).detailsData.loadedDetails
    val headRefName = loadedDetails?.headRefName
    val httpUrl = loadedDetails?.headRepository?.url
    val sshUrl = loadedDetails?.headRepository?.sshUrl
    val isFork = loadedDetails?.headRepository?.isFork ?: false
    val prRemote = GiteeGitHelper.getInstance().findRemote(repository, httpUrl, sshUrl) ?: return
    val localBranch = GiteeGitHelper.getInstance().findLocalBranch(repository, prRemote, isFork, headRefName) ?: return

    GitBranchUtil.updateBranches(project, listOf(repository), listOf(localBranch))
  }
}
