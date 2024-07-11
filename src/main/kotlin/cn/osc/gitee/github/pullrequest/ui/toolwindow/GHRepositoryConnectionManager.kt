// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.github.pullrequest.ui.toolwindow

import git4idea.remote.hosting.HostedGitRepositoryConnectionManager
import git4idea.remote.hosting.ValidatingHostedGitRepositoryConnectionManager
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import cn.osc.gitee.github.api.GHRepositoryConnection
import cn.osc.gitee.github.api.GithubApiRequestExecutor
import cn.osc.gitee.github.authentication.accounts.GHAccountManager
import cn.osc.gitee.github.authentication.accounts.GithubAccount
import cn.osc.gitee.github.pullrequest.data.GHPRDataContextRepository
import cn.osc.gitee.github.util.GHGitRepositoryMapping
import cn.osc.gitee.github.util.GHHostedRepositoriesManager

internal typealias GHRepositoryConnectionManager = HostedGitRepositoryConnectionManager<GHGitRepositoryMapping, GithubAccount, GHRepositoryConnection>

internal fun GHRepositoryConnectionManager(repositoriesManager: GHHostedRepositoriesManager,
                                           accountManager: GHAccountManager,
                                           dataContextRepository: GHPRDataContextRepository): GHRepositoryConnectionManager =
  ValidatingHostedGitRepositoryConnectionManager(repositoriesManager, accountManager) { repo, account, tokenState ->
    val tokenSupplier = GithubApiRequestExecutor.MutableTokenSupplier(tokenState.value)
    launch {
      tokenState.collect {
        tokenSupplier.token = it
      }
    }
    val executor = GithubApiRequestExecutor.Factory.getInstance().create(tokenSupplier)

    val dataContext = dataContextRepository.getContext(repo.repository, repo.remote, account, executor)
    launch(start = CoroutineStart.UNDISPATCHED) {
      try {
        awaitCancellation()
      }
      catch (_: Exception) {
      }
      dataContextRepository.clearContext(repo.repository)
    }
    GHRepositoryConnection(this, repo, account, dataContext)
  }
