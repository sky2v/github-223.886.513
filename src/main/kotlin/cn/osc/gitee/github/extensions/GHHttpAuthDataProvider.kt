// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package cn.osc.gitee.github.extensions

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
import cn.osc.gitee.github.api.GithubApiRequestExecutor
import cn.osc.gitee.github.api.data.GithubAuthenticatedUser
import cn.osc.gitee.github.authentication.GHAccountAuthData
import cn.osc.gitee.github.authentication.accounts.GHAccountManager
import cn.osc.gitee.github.authentication.accounts.GithubAccount
import cn.osc.gitee.github.authentication.accounts.GithubAccountInformationProvider
import cn.osc.gitee.github.authentication.accounts.GithubProjectDefaultAccountHolder

private val LOG = logger<GHHttpAuthDataProvider>()

internal class GHHttpAuthDataProvider : GitHttpAuthDataProvider {
  override fun isSilent(): Boolean = true

  @RequiresBackgroundThread
  override fun getAuthData(project: Project, url: String): GHAccountAuthData? = runBlocking {
    doGetAuthData(project, url)
  }

  private suspend fun doGetAuthData(project: Project, url: String): GHAccountAuthData? {
    val defaultAuthData = getDefaultAccountData(project, url)
    if (defaultAuthData != null) {
      return defaultAuthData
    }

    return getAccountsWithTokens(project, url).entries
      .singleOrNull { it.value != null }?.let { (acc, token) ->
        val login = getAccountDetails(acc, token!!)?.login ?: return null
        GHAccountAuthData(acc, login, token)
      }
  }

  @RequiresBackgroundThread
  override fun getAuthData(project: Project, url: String, login: String): GHAccountAuthData? = runBlocking {
    doGetAuthData(project, url, login)
  }

  private suspend fun doGetAuthData(project: Project, url: String, login: String): GHAccountAuthData? {
    val defaultAuthData = getDefaultAccountData(project, url)
    if (defaultAuthData != null && defaultAuthData.login == login) {
      return defaultAuthData
    }

    return getAccountsWithTokens(project, url).mapNotNull { (acc, token) ->
      if (token == null) return@mapNotNull null
      val details = getAccountDetails(acc, token) ?: return@mapNotNull null
      if (details.login != login) return@mapNotNull null
      GHAccountAuthData(acc, login, token)
    }.singleOrNull()
  }

  override fun forgetPassword(project: Project, url: String, authData: AuthData) {
    if (authData !is GHAccountAuthData) return

    project.service<GHGitAuthenticationFailureManager>().ignoreAccount(url, authData.account)
  }

  companion object {
    private suspend fun getDefaultAccountData(project: Project, url: String): GHAccountAuthData? {
      val defaultAccount = project.service<GithubProjectDefaultAccountHolder>().account ?: return null
      val authFailureManager = project.service<GHGitAuthenticationFailureManager>()

      if (match(defaultAccount.server.toURI(), url) && !authFailureManager.isAccountIgnored(url, defaultAccount)) {
        val token = service<GHAccountManager>().findCredentials(defaultAccount) ?: return null
        val login = getAccountDetails(defaultAccount, token)?.login ?: return null
        return GHAccountAuthData(defaultAccount, login, token)
      }
      return null
    }

    suspend fun getAccountsWithTokens(project: Project, url: String): Map<GithubAccount, String?> {
      val accountManager = service<GHAccountManager>()
      val authFailureManager = project.service<GHGitAuthenticationFailureManager>()

      return accountManager.accountsState.value
        .filter { match(it.server.toURI(), url) }
        .filterNot { authFailureManager.isAccountIgnored(url, it) }
        .associateWith { accountManager.findCredentials(it) }
    }

    suspend fun getAccountDetails(account: GithubAccount, token: String): GithubAuthenticatedUser? =
      try {
        val executor = GithubApiRequestExecutor.Factory.getInstance().create(token)
        withContext(Dispatchers.IO) {
          service<GithubAccountInformationProvider>().getInformation(executor, DumbProgressIndicator(), account)
        }
      }
      catch (e: Exception) {
        if (e !is ProcessCanceledException) LOG.info("Cannot load details for $account", e)
        null
      }
  }
}
