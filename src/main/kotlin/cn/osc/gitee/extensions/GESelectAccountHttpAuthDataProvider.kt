// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.extensions

import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import com.intellij.util.concurrency.annotations.RequiresEdt
import git4idea.DialogManager
import git4idea.i18n.GitBundle
import git4idea.remote.InteractiveGitHttpAuthDataProvider
import cn.osc.gitee.authentication.GEAccountAuthData
import cn.osc.gitee.authentication.GEAccountsUtil
import cn.osc.gitee.authentication.accounts.GiteeAccount
import cn.osc.gitee.authentication.ui.GiteeChooseAccountDialog
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.util.GiteeUtil.GIT_AUTH_PASSWORD_SUBSTITUTE
import java.awt.Component

internal class GESelectAccountHttpAuthDataProvider(
  private val project: Project,
  private val potentialAccounts: Map<GiteeAccount, String?>
) : InteractiveGitHttpAuthDataProvider {

  @RequiresEdt
  override fun getAuthData(parentComponent: Component?): AuthData? {
    val (account, setDefault) = chooseAccount(parentComponent) ?: return null
    val token = potentialAccounts[account]
                ?: GEAccountsUtil.requestNewToken(account, project, parentComponent)
                ?: return null
    if (setDefault) {
      GEAccountsUtil.setDefaultAccount(project, account)
    }

    return GEAccountAuthData(account, GIT_AUTH_PASSWORD_SUBSTITUTE, token)
  }

  private fun chooseAccount(parentComponent: Component?): Pair<GiteeAccount, Boolean>? {
    val dialog = GiteeChooseAccountDialog(
      project, parentComponent,
      potentialAccounts.keys, null, false, true,
      GiteeBundle.message("account.choose.title"), GitBundle.message("login.dialog.button.login")
    )
    DialogManager.show(dialog)

    return if (dialog.isOK) dialog.account to dialog.setDefault else null
  }
}