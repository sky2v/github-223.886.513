// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.github.api

import git4idea.remote.hosting.HostedGitRepositoryConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import cn.osc.gitee.github.authentication.accounts.GithubAccount
import cn.osc.gitee.github.pullrequest.data.GHPRDataContext
import cn.osc.gitee.github.util.GHGitRepositoryMapping

internal class GHRepositoryConnection(private val scope: CoroutineScope,
                                      override val repo: GHGitRepositoryMapping,
                                      override val account: GithubAccount,
                                      val dataContext: GHPRDataContext)
  : HostedGitRepositoryConnection<GHGitRepositoryMapping, GithubAccount> {

  override suspend fun close() {
    try {
      (scope.coroutineContext[Job] ?: error("Missing job")).cancelAndJoin()
    }
    catch (_: Exception) {
    }
  }

  override suspend fun awaitClose() {
    try {
      (scope.coroutineContext[Job] ?: error("Missing job")).join()
    }
    catch (_: Exception) {
    }
  }
}