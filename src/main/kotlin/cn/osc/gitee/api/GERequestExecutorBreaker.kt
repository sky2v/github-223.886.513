// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.NlsSafe

@Service
class GERequestExecutorBreaker {

  @Volatile
  var isRequestsShouldFail = false

  class Action : ToggleAction(actionText), DumbAware {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun isSelected(e: AnActionEvent) =
      service<GERequestExecutorBreaker>().isRequestsShouldFail


    override fun setSelected(e: AnActionEvent, state: Boolean) {
      service<GERequestExecutorBreaker>().isRequestsShouldFail = state
    }

    companion object {
      @NlsSafe
      private val actionText = "Break GitHub API Requests"
    }
  }
}