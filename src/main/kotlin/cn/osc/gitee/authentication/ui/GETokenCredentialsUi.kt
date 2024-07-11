// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.authentication.ui

import com.intellij.ide.BrowserUtil.browse
import com.intellij.openapi.progress.runUnderIndicator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.layout.ComponentPredicate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import cn.osc.gitee.api.GiteeApiRequestExecutor
import cn.osc.gitee.api.GiteeServerPath
import cn.osc.gitee.authentication.util.GESecurityUtil
import cn.osc.gitee.authentication.util.GESecurityUtil.buildNewTokenUrl
import cn.osc.gitee.exceptions.GiteeAuthenticationException
import cn.osc.gitee.exceptions.GiteeParseException
import cn.osc.gitee.i18n.GiteeBundle.message
import cn.osc.gitee.ui.util.DialogValidationUtils.notBlank
import cn.osc.gitee.ui.util.Validator
import java.net.UnknownHostException
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

internal class GETokenCredentialsUi(
  private val serverTextField: ExtendableTextField,
  val factory: GiteeApiRequestExecutor.Factory,
  val isAccountUnique: UniqueLoginPredicate
) : GECredentialsUi() {

  private val tokenTextField = JBPasswordField()
  private var fixedLogin: String? = null

  override fun Panel.centerPanel() {
    row(message("credentials.server.field")) { cell(serverTextField).align(AlignX.FILL) }
    row(message("credentials.token.field")) {
      cell(tokenTextField)
        .comment(message("login.insufficient.scopes", GESecurityUtil.MASTER_SCOPES))
        .align(AlignX.FILL)
        .resizableColumn()
      button(message("credentials.button.generate")) { browseNewTokenUrl() }
        .enabledIf(serverTextField.serverValid)
    }
  }

  private fun browseNewTokenUrl() = browse(buildNewTokenUrl(serverTextField.tryParseServer()!!))

  override fun getPreferredFocusableComponent(): JComponent = tokenTextField

  override fun getValidator(): Validator = { notBlank(tokenTextField, message("login.token.cannot.be.empty")) }

  override suspend fun login(server: GiteeServerPath): Pair<String, String> = withContext(Dispatchers.Main.immediate) {
    val token = tokenTextField.text
    val executor = factory.create(token)
    val login = acquireLogin(server, executor, isAccountUnique, fixedLogin)
    login to token
  }

  override fun handleAcquireError(error: Throwable): ValidationInfo =
    when (error) {
      is GiteeParseException -> ValidationInfo(error.message ?: message("credentials.invalid.server.path"), serverTextField)
      else -> handleError(error)
    }

  override fun setBusy(busy: Boolean) {
    tokenTextField.isEnabled = !busy
  }

  fun setFixedLogin(fixedLogin: String?) {
    this.fixedLogin = fixedLogin
  }

  companion object {
    suspend fun acquireLogin(
      server: GiteeServerPath,
      executor: GiteeApiRequestExecutor,
      isAccountUnique: UniqueLoginPredicate,
      fixedLogin: String?
    ): String {
      val (details, scopes) = withContext(Dispatchers.IO) {
        runUnderIndicator {
          GESecurityUtil.loadCurrentUserWithScopes(executor, server)
        }
      }
      if (scopes == null || !GESecurityUtil.isEnoughScopes(scopes))
        throw GiteeAuthenticationException("Insufficient scopes granted to token.")

      val login = details.login
      if (fixedLogin != null && fixedLogin != login) throw GiteeAuthenticationException("Token should match username \"$fixedLogin\"")
      if (!isAccountUnique(login, server)) throw LoginNotUniqueException(login)

      return login
    }

    fun handleError(error: Throwable): ValidationInfo =
      when (error) {
        is LoginNotUniqueException -> ValidationInfo(message("login.account.already.added", error.login)).withOKEnabled()
        is UnknownHostException -> ValidationInfo(message("server.unreachable")).withOKEnabled()
        is GiteeAuthenticationException -> ValidationInfo(message("credentials.incorrect", error.message.orEmpty())).withOKEnabled()
        else -> ValidationInfo(message("credentials.invalid.auth.data", error.message.orEmpty())).withOKEnabled()
      }
  }
}

private val JTextField.serverValid: ComponentPredicate
  get() = object : ComponentPredicate() {
    override fun invoke(): Boolean = tryParseServer() != null

    override fun addListener(listener: (Boolean) -> Unit) =
      document.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) = listener(tryParseServer() != null)
      })
  }

private fun JTextField.tryParseServer(): GiteeServerPath? =
  try {
    GiteeServerPath.from(text.trim())
  }
  catch (e: GiteeParseException) {
    null
  }