// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package cn.osc.gitee.extensions

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import git4idea.remote.GitHttpAuthDataProvider
import git4idea.remote.hosting.GitHostingUrlUtil.match
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import cn.osc.gitee.api.GiteeApiRequestExecutor
import cn.osc.gitee.api.data.GiteeAuthenticatedUser
import cn.osc.gitee.authentication.GEAccountAuthData
import cn.osc.gitee.authentication.accounts.GEAccountManager
import cn.osc.gitee.authentication.accounts.GiteeAccount
import cn.osc.gitee.authentication.accounts.GiteeAccountInformationProvider
import cn.osc.gitee.authentication.accounts.GiteeProjectDefaultAccountHolder

private val LOG = logger<GEHttpAuthDataProvider>()

internal class GEHttpAuthDataProvider : GitHttpAuthDataProvider {
  override fun isSilent(): Boolean = true

  @RequiresBackgroundThread
  override fun getAuthData(project: Project, url: String): GEAccountAuthData? = runBlocking {
    doGetAuthData(project, url)
  }

  private suspend fun doGetAuthData(project: Project, url: String): GEAccountAuthData? {
    val defaultAuthData = getDefaultAccountData(project, url)
    if (defaultAuthData != null) {
      return defaultAuthData
    }

    return getAccountsWithTokens(project, url).entries
      .singleOrNull { it.value != null }?.let { (acc, token) ->
        val login = getAccountDetails(acc, token!!)?.login ?: return null
        GEAccountAuthData(acc, login, token)
      }
  }

  @RequiresBackgroundThread
  override fun getAuthData(project: Project, url: String, login: String): GEAccountAuthData? = runBlocking {
    doGetAuthData(project, url, login)
  }

  private suspend fun doGetAuthData(project: Project, url: String, login: String): GEAccountAuthData? {
    val defaultAuthData = getDefaultAccountData(project, url)
    if (defaultAuthData != null && defaultAuthData.login == login) {
      return defaultAuthData
    }

    return getAccountsWithTokens(project, url).mapNotNull { (acc, token) ->
      if (token == null) return@mapNotNull null
      val details = getAccountDetails(acc, token) ?: return@mapNotNull null
      if (details.login != login) return@mapNotNull null
      GEAccountAuthData(acc, login, token)
    }.singleOrNull()
  }

  override fun forgetPassword(project: Project, url: String, authData: AuthData) {
    if (authData !is GEAccountAuthData) return

    project.service<GEGitAuthenticationFailureManager>().ignoreAccount(url, authData.account)
  }

  companion object {
    private suspend fun getDefaultAccountData(project: Project, url: String): GEAccountAuthData? {
      val defaultAccount = project.service<GiteeProjectDefaultAccountHolder>().account ?: return null
      val authFailureManager = project.service<GEGitAuthenticationFailureManager>()

      if (match(defaultAccount.server.toURI(), url) && !authFailureManager.isAccountIgnored(url, defaultAccount)) {
        val token = service<GEAccountManager>().findCredentials(defaultAccount) ?: return null
        val login = getAccountDetails(defaultAccount, token)?.login ?: return null
        return GEAccountAuthData(defaultAccount, login, token)
      }
      return null
    }

    suspend fun getAccountsWithTokens(project: Project, url: String): Map<GiteeAccount, String?> {
      val accountManager = service<GEAccountManager>()
      val authFailureManager = project.service<GEGitAuthenticationFailureManager>()

      return accountManager.accountsState.value
        .filter { match(it.server.toURI(), url) }
        .filterNot { authFailureManager.isAccountIgnored(url, it) }
        .associateWith { accountManager.findCredentials(it) }
    }

    suspend fun getAccountDetails(account: GiteeAccount, token: String): GiteeAuthenticatedUser? =
      try {
        val executor = GiteeApiRequestExecutor.Factory.getInstance().create(token)
        withContext(Dispatchers.IO) {
          service<GiteeAccountInformationProvider>().getInformation(executor, DumbProgressIndicator(), account)
        }
      }
      catch (e: Exception) {
        if (e !is ProcessCanceledException) LOG.info("Cannot load details for $account", e)
        null
      }
  }
}
