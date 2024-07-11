// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.authentication.ui

import com.intellij.collaboration.async.DisposingMainScope
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapper.IS_VISUAL_PADDING_COMPENSATED_ON_COMPONENT_LEVEL_KEY
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.update.UiNotifyConnector
import git4idea.i18n.GitBundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import cn.osc.gitee.api.GiteeApiRequestExecutor
import cn.osc.gitee.api.GiteeServerPath
import cn.osc.gitee.i18n.GiteeBundle
import java.awt.Component
import javax.swing.Action
import javax.swing.JComponent

internal fun JComponent.setPaddingCompensated(): JComponent =
  apply { putClientProperty(IS_VISUAL_PADDING_COMPENSATED_ON_COMPONENT_LEVEL_KEY, false) }

internal sealed class GELoginDialog(
  private val model: GELoginModel,
  project: Project?,
  parent: Component?
) : DialogWrapper(project, parent, false, IdeModalityType.PROJECT) {

  private val cs = DisposingMainScope(disposable)

  protected val loginPanel = GiteeLoginPanel(GiteeApiRequestExecutor.Factory.getInstance()) { login, server ->
    model.isAccountUnique(server, login)
  }


  fun setLogin(login: String?, editable: Boolean) = loginPanel.setLogin(login, editable)
  fun setServer(path: String, editable: Boolean) = loginPanel.setServer(path, editable)

  fun setError(exception: Throwable) {
    loginPanel.setError(exception)
    startTrackingValidation()
  }

  override fun getPreferredFocusedComponent(): JComponent? = loginPanel.getPreferredFocusableComponent()

  override fun doValidateAll(): List<ValidationInfo> = loginPanel.doValidateAll()

  override fun doOKAction() {
    cs.launch(Dispatchers.Main.immediate + ModalityState.stateForComponent(rootPane).asContextElement()) {
      try {
        val (login, token) = loginPanel.acquireLoginAndToken()
        model.saveLogin(loginPanel.getServer(), login, token)
        close(OK_EXIT_CODE, true)
      }
      catch (e: Exception) {
        if (e is CancellationException) {
          close(CANCEL_EXIT_CODE, false)
          throw e
        }
        setError(e)
      }
    }
  }


  class Token(model: GELoginModel, project: Project?, parent: Component?) :
    GELoginDialog(model, project, parent) {

    init {
      title = GiteeBundle.message("login.to.github")
      setLoginButtonText(GitBundle.message("login.dialog.button.login"))
      loginPanel.setTokenUi()

      init()
    }

    internal fun setLoginButtonText(@NlsContexts.Button text: String) = setOKButtonText(text)

    override fun createCenterPanel(): JComponent = loginPanel.setPaddingCompensated()
  }

  class OAuth(model: GELoginModel, project: Project?, parent: Component?) :
    GELoginDialog(model, project, parent) {

    init {
      title = GiteeBundle.message("login.to.github")
      loginPanel.setOAuthUi()
      init()
    }

    override fun createActions(): Array<Action> = arrayOf(cancelAction)

    override fun createCenterPanel(): JComponent =
      JBUI.Panels.simplePanel(loginPanel)
        .withPreferredWidth(200)
        .setPaddingCompensated().also {
          UiNotifyConnector.doWhenFirstShown(it) {
            doOKAction()
          }
        }
  }
}

internal interface GELoginModel {
  fun isAccountUnique(server: GiteeServerPath, login: String): Boolean
  suspend fun saveLogin(server: GiteeServerPath, login: String, token: String)
}