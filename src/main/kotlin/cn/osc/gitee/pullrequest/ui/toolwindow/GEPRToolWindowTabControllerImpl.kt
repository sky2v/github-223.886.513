// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.ui.toolwindow

import com.intellij.collaboration.async.collectScoped
import com.intellij.collaboration.async.nestedDisposable
import com.intellij.collaboration.ui.CollaborationToolsUIUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.ClientProperty
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.content.Content
import com.intellij.util.childScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import cn.osc.gitee.i18n.GiteeBundle
import java.awt.BorderLayout
import javax.swing.JPanel

internal class GEPRToolWindowTabControllerImpl(scope: CoroutineScope,
                                               private val project: Project,
                                               private val tabVm: GEPRToolWindowTabViewModel,
                                               private val content: Content) :
  GEPRToolWindowTabController {

  private val cs = scope.childScope(Dispatchers.Main.immediate)

  override var initialView = GEPRToolWindowViewType.LIST
  override val componentController: GEPRToolWindowTabComponentController?
    get() = ClientProperty.findInHierarchy(content.component, GEPRToolWindowTabComponentController.KEY)

  init {
    cs.launch {
      tabVm.viewState.collectScoped { scope, vm ->
        content.displayName = GiteeBundle.message("toolwindow.stripe.Pull_Requests")
        CollaborationToolsUIUtil.setComponentPreservingFocus(content, createNestedComponent(scope, vm))
      }
    }
  }

  private fun createNestedComponent(scope: CoroutineScope, vm: GEPRTabContentViewModel) = when (vm) {
    is GEPRTabContentViewModel.Selectors -> {
      val selector = GERepositoryAndAccountSelectorComponentFactory(project, vm.selectorVm, service()).create(scope)
      JPanel(BorderLayout()).apply {
        add(selector, BorderLayout.NORTH)
      }
    }
    is GEPRTabContentViewModel.PullRequests -> {
      val wrapper = Wrapper()
      GEPRToolWindowTabComponentControllerImpl(project,
                                               project.service(),
                                               project.service(),
                                               vm.connection.dataContext, wrapper, scope.nestedDisposable(),
                                               initialView) {
        content.displayName = it
      }.also {
        ClientProperty.put(wrapper, GEPRToolWindowTabComponentController.KEY, it)
      }
      wrapper
    }
  }

  override fun canResetRemoteOrAccount(): Boolean = tabVm.canSelectDifferentRepoOrAccount()

  override fun resetRemoteAndAccount() = tabVm.selectDifferentRepoOrAccount()
}
