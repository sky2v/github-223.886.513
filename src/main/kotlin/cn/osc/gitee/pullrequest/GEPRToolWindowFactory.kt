// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.pullrequest

import com.intellij.collaboration.async.DisposingMainScope
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.EmptyAction
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.ui.content.Content
import com.intellij.util.childScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import cn.osc.gitee.authentication.accounts.GEAccountManager
import cn.osc.gitee.pullrequest.action.GEPRSelectPullRequestForFileAction
import cn.osc.gitee.pullrequest.action.GEPRSwitchRemoteAction
import cn.osc.gitee.pullrequest.ui.toolwindow.GEPRToolWindowTabController
import cn.osc.gitee.pullrequest.ui.toolwindow.GEPRToolWindowTabControllerImpl
import cn.osc.gitee.pullrequest.ui.toolwindow.GEPRToolWindowTabViewModel
import cn.osc.gitee.pullrequest.ui.toolwindow.GERepositoryConnectionManager
import cn.osc.gitee.util.GEHostedRepositoriesManager
import javax.swing.JPanel

internal class GEPRToolWindowFactory : ToolWindowFactory, DumbAware {
  companion object {
    const val ID = "Pull Requests111"
  }

  override fun init(toolWindow: ToolWindow) {
    val task = toolWindow.project.coroutineScope.launch(start = CoroutineStart.LAZY) {
      val repositoriesManager = toolWindow.project.service<GEHostedRepositoriesManager>()
      withContext(Dispatchers.EDT) {
        repositoriesManager.knownRepositoriesState.collect {
          toolWindow.isAvailable = it.isNotEmpty()
        }
      }
    }
    Disposer.register(toolWindow.disposable, task::cancel)
    task.start()
  }

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    toolWindow.component.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")
    configureToolWindow(toolWindow)
    val contentManager = toolWindow.contentManager
    val content = contentManager.factory.createContent(JPanel(null), null, false).apply {
      isCloseable = false
      setDisposer(Disposer.newDisposable("reviews tab disposable"))
    }
    configureContent(project, content)
    contentManager.addContent(content)
  }

  private fun configureToolWindow(toolWindow: ToolWindow) {
    toolWindow.setTitleActions(listOf(
      EmptyAction.registerWithShortcutSet("Gitee.Create.Pull.Request", CommonShortcuts.getNew(), toolWindow.component),
      GEPRSelectPullRequestForFileAction(),
    ))
    toolWindow.setAdditionalGearActions(DefaultActionGroup(GEPRSwitchRemoteAction()))
  }

  private fun configureContent(project: Project, content: Content) {
    val scope = DisposingMainScope(content)
    val repositoriesManager = project.service<GEHostedRepositoriesManager>()
    val accountManager = service<GEAccountManager>()
    val connectionManager = GERepositoryConnectionManager(repositoriesManager, accountManager, project.service())
    val vm = GEPRToolWindowTabViewModel(scope, repositoriesManager, accountManager, connectionManager, project.service())

    val controller = GEPRToolWindowTabControllerImpl(scope.childScope(), project, vm, content)
    content.putUserData(GEPRToolWindowTabController.KEY, controller)
  }

  override fun shouldBeAvailable(project: Project): Boolean = false
}