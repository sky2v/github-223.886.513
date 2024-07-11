// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.ui

import com.intellij.openapi.project.Project
import cn.osc.gitee.authentication.AuthorizationType
import cn.osc.gitee.authentication.GEAccountsUtil
import cn.osc.gitee.authentication.accounts.GiteeAccount
import cn.osc.gitee.exceptions.GiteeAuthenticationException
import cn.osc.gitee.i18n.GiteeBundle
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action

open class GEApiLoadingErrorHandler(private val project: Project,
                                    private val account: GiteeAccount,
                                    resetRunnable: () -> Unit)
  : GERetryLoadingErrorHandler(resetRunnable) {

  override fun getActionForError(error: Throwable): Action {
    if (error is GiteeAuthenticationException) {
      return ReLoginAction()
    }
    return super.getActionForError(error)
  }

  private inner class ReLoginAction : AbstractAction(GiteeBundle.message("accounts.relogin")) {
    override fun actionPerformed(e: ActionEvent?) {
      if (GEAccountsUtil.requestReLogin(account, project, authType = AuthorizationType.UNDEFINED) != null) {
        resetRunnable()
      }
    }
  }
}