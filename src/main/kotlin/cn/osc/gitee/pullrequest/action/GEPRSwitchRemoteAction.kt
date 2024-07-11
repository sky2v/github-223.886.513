// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.pullrequest.GEPRToolWindowController
import cn.osc.gitee.pullrequest.ui.toolwindow.GEPRToolWindowTabController

class GEPRSwitchRemoteAction : DumbAwareAction(GiteeBundle.message("pull.request.change.remote.or.account")) {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isEnabledAndVisible(e)
  }

  private fun isEnabledAndVisible(e: AnActionEvent): Boolean {
    val controller = e.project?.service<GEPRToolWindowController>()?.getTabController() ?: return false
    return controller.canResetRemoteOrAccount()
  }

  override fun actionPerformed(e: AnActionEvent) = e.project!!.service<GEPRToolWindowController>()
    .activate(GEPRToolWindowTabController::resetRemoteAndAccount)
}