// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.pullrequest.ui.toolwindow

import git4idea.remote.hosting.HostedGitRepositoryConnectionManager
import git4idea.remote.hosting.ValidatingHostedGitRepositoryConnectionManager
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import cn.osc.gitee.api.GERepositoryConnection
import cn.osc.gitee.api.GiteeApiRequestExecutor
import cn.osc.gitee.authentication.accounts.GEAccountManager
import cn.osc.gitee.authentication.accounts.GiteeAccount
import cn.osc.gitee.pullrequest.data.GEPRDataContextRepository
import cn.osc.gitee.util.GEGitRepositoryMapping
import cn.osc.gitee.util.GEHostedRepositoriesManager

internal typealias GERepositoryConnectionManager = HostedGitRepositoryConnectionManager<GEGitRepositoryMapping, GiteeAccount, GERepositoryConnection>

internal fun GERepositoryConnectionManager(repositoriesManager: GEHostedRepositoriesManager,
                                           accountManager: GEAccountManager,
                                           dataContextRepository: GEPRDataContextRepository): GERepositoryConnectionManager =
  ValidatingHostedGitRepositoryConnectionManager(repositoriesManager, accountManager) { repo, account, tokenState ->
    val tokenSupplier = GiteeApiRequestExecutor.MutableTokenSupplier(tokenState.value)
    launch {
      tokenState.collect {
        tokenSupplier.token = it
      }
    }
    val executor = GiteeApiRequestExecutor.Factory.getInstance().create(tokenSupplier)

    val dataContext = dataContextRepository.getContext(repo.repository, repo.remote, account, executor)
    launch(start = CoroutineStart.UNDISPATCHED) {
      try {
        awaitCancellation()
      }
      catch (_: Exception) {
      }
      dataContextRepository.clearContext(repo.repository)
    }
    GERepositoryConnection(this, repo, account, dataContext)
  }
