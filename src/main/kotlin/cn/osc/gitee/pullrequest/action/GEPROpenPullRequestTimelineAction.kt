// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import cn.osc.gitee.i18n.GiteeBundle

class GEPROpenPullRequestTimelineAction
  : DumbAwareAction(GiteeBundle.messagePointer("pull.request.view.conversations.action"),
                    GiteeBundle.messagePointer("pull.request.view.conversations.action.description"),
                    null) {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun update(e: AnActionEvent) {
    val controller = e.getData(GEPRActionKeys.PULL_REQUESTS_TAB_CONTROLLER)
    val selection = e.getData(GEPRActionKeys.SELECTED_PULL_REQUEST)
    val dataProvider = e.getData(GEPRActionKeys.PULL_REQUEST_DATA_PROVIDER)

    e.presentation.isEnabled = controller != null && (selection != null || dataProvider != null)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val controller = e.getRequiredData(GEPRActionKeys.PULL_REQUESTS_TAB_CONTROLLER)
    val selection = e.getData(GEPRActionKeys.SELECTED_PULL_REQUEST)
    val dataProvider = e.getData(GEPRActionKeys.PULL_REQUEST_DATA_PROVIDER)

    val pullRequest = selection ?: dataProvider!!.id

    controller.openPullRequestTimeline(pullRequest, true)
  }
}