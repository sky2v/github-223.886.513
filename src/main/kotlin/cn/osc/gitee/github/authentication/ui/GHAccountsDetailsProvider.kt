// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.github.authentication.ui

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
import cn.osc.gitee.github.GithubIcons
import cn.osc.gitee.github.api.GithubApiRequestExecutor
import cn.osc.gitee.github.api.data.GithubAuthenticatedUser
import cn.osc.gitee.github.api.data.GithubUserDetailed
import cn.osc.gitee.github.authentication.accounts.GHAccountManager
import cn.osc.gitee.github.authentication.accounts.GithubAccount
import cn.osc.gitee.github.authentication.util.GHSecurityUtil
import cn.osc.gitee.github.i18n.GithubBundle
import cn.osc.gitee.github.util.CachingGHUserAvatarLoader
import java.awt.Image

internal class GHAccountsDetailsProvider(
  scope: CoroutineScope,
  private val executorSupplier: suspend (GithubAccount) -> GithubApiRequestExecutor?
) : LazyLoadingAccountsDetailsProvider<GithubAccount, GithubUserDetailed>(scope, GithubIcons.DefaultAvatar) {

  constructor(scope: CoroutineScope, accountManager: GHAccountManager, accountsModel: GHAccountsListModel)
    : this(scope, { getExecutor(accountManager, accountsModel, it) }) {
    cancelOnRemoval(accountsModel.accountsListModel)
  }

  constructor(scope: CoroutineScope, accountManager: GHAccountManager)
    : this(scope, { getExecutor(accountManager, it) }) {
    cancelOnRemoval(scope, accountManager)
  }

  override suspend fun loadDetails(account: GithubAccount): Result<GithubUserDetailed> {
    val executor = try {
      executorSupplier(account)
    }
    catch (e: Exception) {
      null
    }
    if (executor == null) return Result.Error(GithubBundle.message("account.token.missing"), true)
    return withContext(Dispatchers.IO) {
      runUnderIndicator {
        doLoadDetails(executor, account)
      }
    }
  }

  private fun doLoadDetails(executor: GithubApiRequestExecutor, account: GithubAccount)
    : Result<GithubAuthenticatedUser> {

    val (details, scopes) = try {
      GHSecurityUtil.loadCurrentUserWithScopes(executor, account.server)
    }
    catch (e: Throwable) {
      val errorMessage = ExceptionUtil.getPresentableMessage(e)
      return Result.Error(errorMessage, false)
    }
    if (!GHSecurityUtil.isEnoughScopes(scopes.orEmpty())) {
      return Result.Error(GithubBundle.message("account.scopes.insufficient"), true)
    }

    return Result.Success(details)
  }

  override suspend fun loadAvatar(account: GithubAccount, url: String): Image? {
    val apiExecutor = executorSupplier(account) ?: return null
    return CachingGHUserAvatarLoader.getInstance().requestAvatar(apiExecutor, url).await()
  }

  companion object {
    private suspend fun getExecutor(accountManager: GHAccountManager, accountsModel: GHAccountsListModel, account: GithubAccount)
      : GithubApiRequestExecutor? {
      return accountsModel.newCredentials.getOrElse(account) {
        accountManager.findCredentials(account)
      }?.let { token ->
        service<GithubApiRequestExecutor.Factory>().create(token)
      }
    }

    private suspend fun getExecutor(accountManager: GHAccountManager, account: GithubAccount)
      : GithubApiRequestExecutor? {
      return accountManager.findCredentials(account)?.let { token ->
        service<GithubApiRequestExecutor.Factory>().create(token)
      }
    }
  }
}