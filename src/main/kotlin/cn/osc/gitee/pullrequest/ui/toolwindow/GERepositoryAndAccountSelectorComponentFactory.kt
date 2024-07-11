// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.pullrequest.ui.toolwindow

import com.intellij.collaboration.ui.CollaborationToolsUIUtil.isDefault
import com.intellij.collaboration.ui.util.bindDisabled
import com.intellij.collaboration.ui.util.bindVisibility
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import git4idea.remote.hosting.ui.RepositoryAndAccountSelectorComponentFactory
import kotlinx.coroutines.CoroutineScope
import cn.osc.gitee.api.GiteeServerPath
import cn.osc.gitee.authentication.AuthorizationType
import cn.osc.gitee.authentication.GEAccountsUtil
import cn.osc.gitee.authentication.accounts.GEAccountManager
import cn.osc.gitee.authentication.ui.GEAccountsDetailsProvider
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.ui.util.GEUIUtil
import cn.osc.gitee.util.GEGitRepositoryMapping
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JComponent

class GERepositoryAndAccountSelectorComponentFactory internal constructor(private val project: Project,
                                                                          private val vm: GERepositoryAndAccountSelectorViewModel,
                                                                          private val accountManager: GEAccountManager) {

  fun create(scope: CoroutineScope): JComponent {
    val accountDetailsProvider = GEAccountsDetailsProvider(scope, accountManager)

    return RepositoryAndAccountSelectorComponentFactory(vm)
      .create(scope = scope,
              repoNamer = { mapping ->
                val allRepositories = vm.repositoriesState.value.map { it.repository }
                GEUIUtil.getRepositoryDisplayName(allRepositories, mapping.repository, true)
              },
              detailsProvider = accountDetailsProvider,
              accountsPopupActionsSupplier = { createPopupLoginActions(it) },
              credsMissingText = GiteeBundle.message("account.token.missing"),
              submitActionText = GiteeBundle.message("pull.request.view.list"),
              loginButtons = createLoginButtons(scope))
  }

  private fun createLoginButtons(scope: CoroutineScope): List<JButton> {
    return listOf(
      JButton(GiteeBundle.message("action.Gitee.Accounts.AddGHAccount.text")).apply {
        isDefault = true
        isOpaque = false

        addActionListener {
          if (loginToGithub(false, AuthorizationType.OAUTH)) {
            vm.submitSelection()
          }
        }

        bindVisibility(scope, vm.githubLoginAvailableState)
        bindDisabled(scope, vm.busyState)
      },

      ActionLink(GiteeBundle.message("action.Gitee.Accounts.AddGHAccountWithToken.text")) {
        if (loginToGithub(false, AuthorizationType.TOKEN)) {
          vm.submitSelection()
        }
      }.apply {

        bindVisibility(scope, vm.githubLoginAvailableState)
        autoHideOnDisable = false
        bindDisabled(scope, vm.busyState)
      },
      JButton(GiteeBundle.message("action.Gitee.Accounts.AddGHEAccount.text")).apply {
        isDefault = true
        isOpaque = false

        addActionListener {
          val repo = vm.repoSelectionState.value ?: return@addActionListener
          if (loginToGhe(false, repo)) {
            vm.submitSelection()
          }
        }

        bindVisibility(scope, vm.gheLoginAvailableState)
        bindDisabled(scope, vm.busyState)
      }
    )
  }

  private fun createPopupLoginActions(repo: GEGitRepositoryMapping?): List<AbstractAction> {
    val isDotComServer = repo?.repository?.serverPath?.isGithubDotCom ?: false
    return if (isDotComServer)
      listOf(object : AbstractAction(GiteeBundle.message("action.Gitee.Accounts.AddGHAccount.text")) {
        override fun actionPerformed(e: ActionEvent?) {
          loginToGithub(true, AuthorizationType.OAUTH)
        }
      }, object : AbstractAction(GiteeBundle.message("action.Gitee.Accounts.AddGHAccountWithToken.text")) {
        override fun actionPerformed(e: ActionEvent?) {
          loginToGithub(true, AuthorizationType.TOKEN)
        }
      })
    else listOf(
      object : AbstractAction(GiteeBundle.message("action.Gitee.Accounts.AddGHEAccount.text")) {
        override fun actionPerformed(e: ActionEvent?) {
          loginToGhe(true, repo!!)
        }
      })
  }

  private fun loginToGithub(forceNew: Boolean, authType: AuthorizationType): Boolean {
    val account = vm.accountSelectionState.value
    if (account == null || forceNew) {
      return GEAccountsUtil.requestNewAccount(GiteeServerPath.DEFAULT_SERVER,
                                              null,
                                              project,
                                              authType = authType
      )?.account?.also {
        vm.accountSelectionState.value = it
      } != null
    }
    else if (vm.missingCredentialsState.value == true) {
      return GEAccountsUtil.requestReLogin(account, project, authType = authType) != null
    }
    return false
  }

  private fun loginToGhe(forceNew: Boolean, repo: GEGitRepositoryMapping): Boolean {
    val server = repo.repository.serverPath
    val account = vm.accountSelectionState.value
    if (account == null || forceNew) {
      return GEAccountsUtil.requestNewAccount(server, login = null, project = project)?.also {
        vm.accountSelectionState.value = it.account
      } != null
    }
    else if (vm.missingCredentialsState.value == true) {
      return GEAccountsUtil.requestReLogin(account, project, authType = AuthorizationType.TOKEN) != null
    }
    return false
  }
}

