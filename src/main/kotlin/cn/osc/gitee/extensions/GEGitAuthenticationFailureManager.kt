// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.extensions

import com.intellij.collaboration.async.disposingScope
import com.intellij.collaboration.auth.AccountUrlAuthenticationFailuresHolder
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Disposer
import cn.osc.gitee.authentication.accounts.GEAccountManager
import cn.osc.gitee.authentication.accounts.GiteeAccount

internal class GEGitAuthenticationFailureManager : Disposable {
  private val holder = AccountUrlAuthenticationFailuresHolder(disposingScope()) {
    service<GEAccountManager>()
  }.also {
    Disposer.register(this, it)
  }

  fun ignoreAccount(url: String, account: GiteeAccount) {
    holder.markFailed(account, url)
  }

  fun isAccountIgnored(url: String, account: GiteeAccount): Boolean = holder.isFailed(account, url)

  override fun dispose() = Unit
}