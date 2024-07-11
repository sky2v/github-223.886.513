// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.authentication

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.components.DropDownLink
import com.intellij.util.AuthData
import com.intellij.util.concurrency.annotations.RequiresEdt
import git4idea.DialogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import cn.osc.gitee.api.GiteeServerPath
import cn.osc.gitee.authentication.accounts.GEAccountManager
import cn.osc.gitee.authentication.accounts.GiteeAccount
import cn.osc.gitee.authentication.accounts.GiteeProjectDefaultAccountHolder
import cn.osc.gitee.authentication.ui.GELoginDialog
import cn.osc.gitee.authentication.ui.GELoginModel
import cn.osc.gitee.i18n.GiteeBundle
import java.awt.Component
import javax.swing.JButton
import javax.swing.JComponent

private val accountManager: GEAccountManager get() = service()

object GEAccountsUtil {
  @JvmStatic
  val accounts: Set<GiteeAccount>
    get() = accountManager.accountsState.value

  @JvmStatic
  fun getDefaultAccount(project: Project): GiteeAccount? =
    project.service<GiteeProjectDefaultAccountHolder>().account

  @JvmStatic
  fun setDefaultAccount(project: Project, account: GiteeAccount?) {
    project.service<GiteeProjectDefaultAccountHolder>().account = account
  }

  @JvmStatic
  fun getSingleOrDefaultAccount(project: Project): GiteeAccount? =
    getDefaultAccount(project) ?: accounts.singleOrNull()

  fun createAddAccountLink(project: Project, accountSelectionModel: CollectionComboBoxModel<GiteeAccount>): JButton {
    val model = object : GELoginModel {
      override fun isAccountUnique(server: GiteeServerPath, login: String): Boolean =
        accountSelectionModel.items.none { it.name == login && it.server.equals(server, true) }

      override suspend fun saveLogin(server: GiteeServerPath, login: String, token: String) {
        val account = GEAccountManager.createAccount(login, server)
        accountManager.updateAccount(account, token)
        withContext(Dispatchers.Main.immediate) {
          accountSelectionModel.add(account)
          accountSelectionModel.selectedItem = account
        }
      }
    }

    return DropDownLink(GiteeBundle.message("accounts.add.dropdown.link")) {
      val group = createAddAccountActionGroup(model, project, it)
      JBPopupFactory.getInstance()
        .createActionGroupPopup(null, group, DataManager.getInstance().getDataContext(it),
                                JBPopupFactory.ActionSelectionAid.MNEMONICS, false)
    }
  }

  internal fun createAddAccountActionGroup(model: GELoginModel, project: Project, parentComponent: JComponent): ActionGroup {
    val group = DefaultActionGroup()
    group.add(
      DumbAwareAction.create(GiteeBundle.message("action.Gitee.Accounts.AddGHAccount.text")) {
        GELoginDialog.OAuth(model, project, parentComponent).apply {
          setServer(GiteeServerPath.DEFAULT_HOST, false)
          showAndGet()
        }
      })

    group.add(
      DumbAwareAction.create(GiteeBundle.message("action.Gitee.Accounts.AddGHAccountWithToken.text")) {
        GELoginDialog.Token(model, project, parentComponent).apply {
          title = GiteeBundle.message("dialog.title.add.github.account")
          setLoginButtonText(GiteeBundle.message("accounts.add.button"))
          setServer(GiteeServerPath.DEFAULT_HOST, false)
          showAndGet()
        }
      }
    )

    group.add(Separator())

    group.add(
      DumbAwareAction.create(GiteeBundle.message("action.Gitee.Accounts.AddGHEAccount.text")) {
        GELoginDialog.Token(model, project, parentComponent).apply {
          title = GiteeBundle.message("dialog.title.add.github.account")
          setServer("", true)
          setLoginButtonText(GiteeBundle.message("accounts.add.button"))
          showAndGet()
        }
      }
    )
    return group
  }

  @RequiresEdt
  @JvmOverloads
  @JvmStatic
  internal fun requestNewToken(
    account: GiteeAccount,
    project: Project?,
    parentComponent: Component? = null
  ): String? {
    val model = AccountManagerLoginModel(account)
    login(
      model,
      GELoginRequest(
        text = GiteeBundle.message("account.token.missing.for", account),
        server = account.server, login = account.name
      ),
      project, parentComponent,
    )
    return model.authData?.token
  }

  @RequiresEdt
  @JvmOverloads
  @JvmStatic
  fun requestReLogin(
    account: GiteeAccount,
    project: Project?,
    parentComponent: Component? = null,
    authType: AuthorizationType = AuthorizationType.UNDEFINED
  ): GEAccountAuthData? {
    val model = AccountManagerLoginModel(account)
    login(
      model, GELoginRequest(server = account.server, login = account.name, authType = authType),
      project, parentComponent)
    return model.authData
  }

  @RequiresEdt
  @JvmOverloads
  @JvmStatic
  fun requestNewAccount(
    server: GiteeServerPath? = null,
    login: String? = null,
    project: Project?,
    parentComponent: Component? = null,
    authType: AuthorizationType = AuthorizationType.UNDEFINED
  ): GEAccountAuthData? {
    val model = AccountManagerLoginModel()
    login(
      model, GELoginRequest(server = server, login = login, isLoginEditable = login != null, authType = authType),
      project, parentComponent
    )
    return model.authData
  }

  @RequiresEdt
  @JvmStatic
  internal fun login(model: GELoginModel, request: GELoginRequest, project: Project?, parentComponent: Component?) {
    if (request.server != GiteeServerPath.DEFAULT_SERVER) {
      request.loginWithToken(model, project, parentComponent)
    }
    else when (request.authType) {
      AuthorizationType.OAUTH -> request.loginWithOAuth(model, project, parentComponent)
      AuthorizationType.TOKEN -> request.loginWithToken(model, project, parentComponent)
      AuthorizationType.UNDEFINED -> request.loginWithOAuthOrToken(model, project, parentComponent)
    }
  }
}

class GEAccountAuthData(val account: GiteeAccount, login: String, token: String) : AuthData(login, token) {
  val server: GiteeServerPath get() = account.server
  val token: String get() = password!!
}

internal class GELoginRequest(
  val text: @NlsContexts.DialogMessage String? = null,
  val error: Throwable? = null,

  val server: GiteeServerPath? = null,
  val isServerEditable: Boolean = server == null,

  val login: String? = null,
  val isLoginEditable: Boolean = true,

  val authType: AuthorizationType = AuthorizationType.UNDEFINED
)

private fun GELoginRequest.configure(dialog: GELoginDialog) {
  error?.let { dialog.setError(it) }
  server?.let { dialog.setServer(it.toString(), isServerEditable) }
  login?.let { dialog.setLogin(it, isLoginEditable) }
}

private fun GELoginRequest.loginWithToken(model: GELoginModel, project: Project?, parentComponent: Component?) {
  val dialog = GELoginDialog.Token(model, project, parentComponent)
  configure(dialog)
  DialogManager.show(dialog)
}

private fun GELoginRequest.loginWithOAuth(model: GELoginModel, project: Project?, parentComponent: Component?) {
  val dialog = GELoginDialog.OAuth(model, project, parentComponent)
  configure(dialog)
  DialogManager.show(dialog)
}

private fun GELoginRequest.loginWithOAuthOrToken(model: GELoginModel, project: Project?, parentComponent: Component?) {
  when (promptOAuthLogin(this, project, parentComponent)) {
    Messages.YES -> loginWithOAuth(model, project, parentComponent)
    Messages.NO -> loginWithToken(model, project, parentComponent)
  }
}

private fun promptOAuthLogin(request: GELoginRequest, project: Project?, parentComponent: Component?): Int {
  val builder = MessageDialogBuilder.yesNoCancel(title = GiteeBundle.message("login.to.github"),
                                                 message = request.text ?: GiteeBundle.message("dialog.message.login.to.continue"))
    .yesText(GiteeBundle.message("login.via.github.action"))
    .noText(GiteeBundle.message("button.use.token"))
    .icon(Messages.getWarningIcon())
  if (parentComponent != null) {
    return builder.show(parentComponent)
  }
  else {
    return builder.show(project)
  }
}

private class AccountManagerLoginModel(private val account: GiteeAccount? = null) : GELoginModel {
  private val accountManager: GEAccountManager = service()

  var authData: GEAccountAuthData? = null

  override fun isAccountUnique(server: GiteeServerPath, login: String): Boolean =
    accountManager.accountsState.value.filter {
      it != account
    }.none {
      it.name == login && it.server.equals(server, true)
    }

  override suspend fun saveLogin(server: GiteeServerPath, login: String, token: String) {
    val acc = account ?: GEAccountManager.createAccount(login, server)
    acc.name = login
    accountManager.updateAccount(acc, token)
    authData = GEAccountAuthData(acc, login, token)
  }
}