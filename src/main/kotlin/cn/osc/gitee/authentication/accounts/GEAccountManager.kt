// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.authentication.accounts

import com.intellij.collaboration.auth.AccountManagerBase
import com.intellij.collaboration.auth.PasswordSafeCredentialsRepository
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import cn.osc.gitee.api.GiteeServerPath
import cn.osc.gitee.util.GiteeUtil

internal val GiteeAccount.isGHAccount: Boolean get() = server.isGithubDotCom

/**
 * Handles application-level Gitee accounts
 */
@Service
internal class GEAccountManager
  : AccountManagerBase<GiteeAccount, String>(logger<GEAccountManager>()), Disposable {

  override fun accountsRepository() = service<GEPersistentAccounts>()

  override fun credentialsRepository() =
    PasswordSafeCredentialsRepository<GiteeAccount, String>(GiteeUtil.SERVICE_DISPLAY_NAME,
                                                             PasswordSafeCredentialsRepository.CredentialsMapper.Simple)

  companion object {
    fun createAccount(name: String, server: GiteeServerPath) = GiteeAccount(name, server)
  }

  override fun dispose() = Unit
}