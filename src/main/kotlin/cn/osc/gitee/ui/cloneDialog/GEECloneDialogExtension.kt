// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.ui.cloneDialog

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI.Panels.simplePanel
import com.intellij.util.ui.UIUtil.ComponentStyle
import com.intellij.util.ui.UIUtil.getRegularPanelInsets
import com.intellij.util.ui.cloneDialog.AccountMenuItem
import com.intellij.util.ui.components.BorderLayoutPanel
import cn.osc.gitee.authentication.GEAccountsUtil
import cn.osc.gitee.authentication.accounts.GiteeAccount
import cn.osc.gitee.authentication.accounts.isGHAccount
import cn.osc.gitee.i18n.GiteeBundle.message
import cn.osc.gitee.util.GiteeUtil
import javax.swing.JComponent

private val GiteeAccount.isGHEAccount: Boolean get() = !isGHAccount

class GEECloneDialogExtension : BaseCloneDialogExtension() {
  override fun getName(): String = GiteeUtil.ENTERPRISE_SERVICE_DISPLAY_NAME

  override fun getAccounts(): Collection<GiteeAccount> = GEAccountsUtil.accounts.filter { it.isGHEAccount }

  override fun createMainComponent(project: Project, modalityState: ModalityState): VcsCloneDialogExtensionComponent =
    GEECloneDialogExtensionComponent(project, modalityState)
}

private class GEECloneDialogExtensionComponent(project: Project, modalityState: ModalityState) : GECloneDialogExtensionComponentBase(
  project,
  modalityState,
  accountManager = service()
) {

  override fun isAccountHandled(account: GiteeAccount): Boolean = account.isGHEAccount

  override fun createLoginPanel(account: GiteeAccount?, cancelHandler: () -> Unit): JComponent =
    GEECloneDialogLoginPanel(account).apply {
      Disposer.register(this@GEECloneDialogExtensionComponent, this)

      loginPanel.isCancelVisible = getAccounts().isNotEmpty()
      loginPanel.setCancelHandler(cancelHandler)
    }

  override fun createAccountMenuLoginActions(account: GiteeAccount?): Collection<AccountMenuItem.Action> =
    listOf(createLoginAction(account))

  private fun createLoginAction(account: GiteeAccount?): AccountMenuItem.Action {
    val isExistingAccount = account != null
    return AccountMenuItem.Action(
      message("login.to.github.enterprise.action"),
      { switchToLogin(account) },
      showSeparatorAbove = !isExistingAccount
    )
  }
}

private class GEECloneDialogLoginPanel(account: GiteeAccount?) : BorderLayoutPanel(), Disposable {
  private val titlePanel =
    simplePanel().apply {
      val title = JBLabel(message("login.to.github.enterprise"), ComponentStyle.LARGE).apply { font = JBFont.label().biggerOn(5.0f) }
      addToLeft(title)
    }
  val loginPanel = CloneDialogLoginPanel(account).apply {
    Disposer.register(this@GEECloneDialogLoginPanel, this)

    if (account == null) setServer("", true)
    setTokenUi()
  }

  init {
    addToTop(titlePanel.apply { border = JBEmptyBorder(getRegularPanelInsets().apply { bottom = 0 }) })
    addToCenter(loginPanel)
  }

  override fun dispose() = loginPanel.cancelLogin()
}