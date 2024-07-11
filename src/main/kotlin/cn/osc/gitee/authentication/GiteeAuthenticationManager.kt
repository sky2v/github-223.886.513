// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.authentication

import com.intellij.collaboration.async.collectWithPrevious
import com.intellij.collaboration.async.disposingMainScope
import com.intellij.collaboration.auth.AccountsListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.annotations.CalledInAny
import cn.osc.gitee.authentication.accounts.GEAccountManager
import cn.osc.gitee.authentication.accounts.GiteeAccount
import java.awt.Component


/**
 * Entry point for interactions with Gitee authentication subsystem
 */
@Deprecated("deprecated in favor of GEAccountsUtil")
class GiteeAuthenticationManager internal constructor() {
  private val accountManager: GEAccountManager get() = service()

  @CalledInAny
  fun getAccounts(): Set<GiteeAccount> = accountManager.accountsState.value

  @CalledInAny
  fun hasAccounts() = accountManager.accountsState.value.isNotEmpty()

  @RequiresEdt
  @JvmOverloads
  fun ensureHasAccounts(project: Project?, parentComponent: Component? = null): Boolean {
    if (accountManager.accountsState.value.isNotEmpty()) return true
    return GEAccountsUtil.requestNewAccount(project = project, parentComponent = parentComponent) != null
  }

  fun getSingleOrDefaultAccount(project: Project): GiteeAccount? = GEAccountsUtil.getSingleOrDefaultAccount(project)

  @Deprecated("replaced with stateFlow", ReplaceWith("accountManager.accountsState"))
  @RequiresEdt
  fun addListener(disposable: Disposable, listener: AccountsListener<GiteeAccount>) {
    disposable.disposingMainScope().launch {
      accountManager.accountsState.collectWithPrevious(setOf()) { prev, current ->
        listener.onAccountListChanged(prev, current)
        current.forEach { acc ->
          async {
            accountManager.getCredentialsFlow(acc).collectLatest {
              listener.onAccountCredentialsChanged(acc)
            }
          }
        }
      }
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(): GiteeAuthenticationManager = service()
  }
}