// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package cn.osc.gitee.github.pullrequest.action

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.actionSystem.AnActionEvent
import cn.osc.gitee.github.i18n.GithubBundle
import java.util.function.Supplier

class GHPRReloadListAction
  : RefreshAction(GithubBundle.messagePointer("pull.request.refresh.list.action"),
                  Supplier<String?> { null },
                  AllIcons.Actions.Refresh) {

  override fun update(e: AnActionEvent) {
    val controller = e.getData(GHPRActionKeys.PULL_REQUESTS_TAB_CONTROLLER)
    e.presentation.isEnabled = controller != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    e.getRequiredData(GHPRActionKeys.PULL_REQUESTS_TAB_CONTROLLER).refreshList()
  }
}