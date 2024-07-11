// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.api

import git4idea.remote.hosting.HostedGitRepositoryConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import cn.osc.gitee.authentication.accounts.GiteeAccount
import cn.osc.gitee.pullrequest.data.GEPRDataContext
import cn.osc.gitee.util.GEGitRepositoryMapping

internal class GERepositoryConnection(private val scope: CoroutineScope,
                                      override val repo: GEGitRepositoryMapping,
                                      override val account: GiteeAccount,
                                      val dataContext: GEPRDataContext)
  : HostedGitRepositoryConnection<GEGitRepositoryMapping, GiteeAccount> {

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