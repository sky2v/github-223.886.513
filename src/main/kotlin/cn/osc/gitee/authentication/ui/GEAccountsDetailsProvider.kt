// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.authentication.ui

import com.intellij.collaboration.auth.ui.LazyLoadingAccountsDetailsProvider
import com.intellij.collaboration.auth.ui.cancelOnRemoval
import com.intellij.collaboration.ui.ExceptionUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.runUnderIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import cn.osc.gitee.GiteeIcons
import cn.osc.gitee.api.GiteeApiRequestExecutor
import cn.osc.gitee.api.data.GiteeAuthenticatedUser
import cn.osc.gitee.api.data.GiteeUserDetailed
import cn.osc.gitee.authentication.accounts.GEAccountManager
import cn.osc.gitee.authentication.accounts.GiteeAccount
import cn.osc.gitee.authentication.util.GESecurityUtil
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.util.CachingGHUserAvatarLoader
import java.awt.Image

internal class GEAccountsDetailsProvider(
  scope: CoroutineScope,
  private val executorSupplier: suspend (GiteeAccount) -> GiteeApiRequestExecutor?
) : LazyLoadingAccountsDetailsProvider<GiteeAccount, GiteeUserDetailed>(scope, GiteeIcons.DefaultAvatar) {

  constructor(scope: CoroutineScope, accountManager: GEAccountManager, accountsModel: GEAccountsListModel)
    : this(scope, { getExecutor(accountManager, accountsModel, it) }) {
    cancelOnRemoval(accountsModel.accountsListModel)
  }

  constructor(scope: CoroutineScope, accountManager: GEAccountManager)
    : this(scope, { getExecutor(accountManager, it) }) {
    cancelOnRemoval(scope, accountManager)
  }

  override suspend fun loadDetails(account: GiteeAccount): Result<GiteeUserDetailed> {
    val executor = try {
      executorSupplier(account)
    }
    catch (e: Exception) {
      null
    }
    if (executor == null) return Result.Error(GiteeBundle.message("account.token.missing"), true)
    return withContext(Dispatchers.IO) {
      runUnderIndicator {
        doLoadDetails(executor, account)
      }
    }
  }

  private fun doLoadDetails(executor: GiteeApiRequestExecutor, account: GiteeAccount)
    : Result<GiteeAuthenticatedUser> {

    val (details, scopes) = try {
      GESecurityUtil.loadCurrentUserWithScopes(executor, account.server)
    }
    catch (e: Throwable) {
      val errorMessage = ExceptionUtil.getPresentableMessage(e)
      return Result.Error(errorMessage, false)
    }
    if (!GESecurityUtil.isEnoughScopes(scopes.orEmpty())) {
      return Result.Error(GiteeBundle.message("account.scopes.insufficient"), true)
    }

    return Result.Success(details)
  }

  override suspend fun loadAvatar(account: GiteeAccount, url: String): Image? {
    val apiExecutor = executorSupplier(account) ?: return null
    return CachingGHUserAvatarLoader.getInstance().requestAvatar(apiExecutor, url).await()
  }

  companion object {
    private suspend fun getExecutor(accountManager: GEAccountManager, accountsModel: GEAccountsListModel, account: GiteeAccount)
      : GiteeApiRequestExecutor? {
      return accountsModel.newCredentials.getOrElse(account) {
        accountManager.findCredentials(account)
      }?.let { token ->
        service<GiteeApiRequestExecutor.Factory>().create(token)
      }
    }

    private suspend fun getExecutor(accountManager: GEAccountManager, account: GiteeAccount)
      : GiteeApiRequestExecutor? {
      return accountManager.findCredentials(account)?.let { token ->
        service<GiteeApiRequestExecutor.Factory>().create(token)
      }
    }
  }
}