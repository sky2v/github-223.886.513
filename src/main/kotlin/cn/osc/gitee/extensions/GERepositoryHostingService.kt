// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.extensions

import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import git4idea.remote.GitRepositoryHostingService
import git4idea.remote.InteractiveGitHttpAuthDataProvider
import kotlinx.coroutines.runBlocking
import cn.osc.gitee.util.GiteeUtil

internal class GERepositoryHostingService : GitRepositoryHostingService() {
  override fun getServiceDisplayName(): String = GiteeUtil.SERVICE_DISPLAY_NAME

  @RequiresBackgroundThread
  override fun getInteractiveAuthDataProvider(project: Project, url: String)
    : InteractiveGitHttpAuthDataProvider? = runBlocking {
    GEHttpAuthDataProvider.getAccountsWithTokens(project, url).takeIf { it.isNotEmpty() }?.let {
      GESelectAccountHttpAuthDataProvider(project, it)
    }
  }

  @RequiresBackgroundThread
  override fun getInteractiveAuthDataProvider(project: Project, url: String, login: String)
    : InteractiveGitHttpAuthDataProvider? = runBlocking {
    GEHttpAuthDataProvider.getAccountsWithTokens(project, url).mapNotNull { (acc, token) ->
      if (token == null) return@mapNotNull null
      val details = GEHttpAuthDataProvider.getAccountDetails(acc, token) ?: return@mapNotNull null
      if (details.login != login) return@mapNotNull null
      acc to token
    }.takeIf { it.isNotEmpty() }?.let {
      GESelectAccountHttpAuthDataProvider(project, it.toMap())
    }
  }
}