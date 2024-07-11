// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.extensions

import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import com.intellij.util.concurrency.annotations.RequiresEdt
import git4idea.DialogManager
import git4idea.i18n.GitBundle
import git4idea.remote.InteractiveGitHttpAuthDataProvider
import cn.osc.gitee.github.authentication.GHAccountAuthData
import cn.osc.gitee.github.authentication.GHAccountsUtil
import cn.osc.gitee.github.authentication.accounts.GithubAccount
import cn.osc.gitee.github.authentication.ui.GithubChooseAccountDialog
import cn.osc.gitee.github.i18n.GithubBundle
import cn.osc.gitee.github.util.GithubUtil.GIT_AUTH_PASSWORD_SUBSTITUTE
import java.awt.Component

internal class GHSelectAccountHttpAuthDataProvider(
  private val project: Project,
  private val potentialAccounts: Map<GithubAccount, String?>
) : InteractiveGitHttpAuthDataProvider {

  @RequiresEdt
  override fun getAuthData(parentComponent: Component?): AuthData? {
    val (account, setDefault) = chooseAccount(parentComponent) ?: return null
    val token = potentialAccounts[account]
                ?: GHAccountsUtil.requestNewToken(account, project, parentComponent)
                ?: return null
    if (setDefault) {
      GHAccountsUtil.setDefaultAccount(project, account)
    }

    return GHAccountAuthData(account, GIT_AUTH_PASSWORD_SUBSTITUTE, token)
  }

  private fun chooseAccount(parentComponent: Component?): Pair<GithubAccount, Boolean>? {
    val dialog = GithubChooseAccountDialog(
      project, parentComponent,
      potentialAccounts.keys, null, false, true,
      GithubBundle.message("account.choose.title"), GitBundle.message("login.dialog.button.login")
    )
    DialogManager.show(dialog)

    return if (dialog.isOK) dialog.account to dialog.setDefault else null
  }
}