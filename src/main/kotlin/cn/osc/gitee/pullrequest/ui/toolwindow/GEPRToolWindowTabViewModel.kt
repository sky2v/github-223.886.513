// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.pullrequest.ui.toolwindow

import com.intellij.collaboration.async.combineState
import com.intellij.collaboration.async.mapStateScoped
import com.intellij.collaboration.util.URIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import cn.osc.gitee.api.GERepositoryConnection
import cn.osc.gitee.authentication.accounts.GEAccountManager
import cn.osc.gitee.authentication.accounts.GiteeAccount
import cn.osc.gitee.pullrequest.config.GiteePullRequestsProjectUISettings
import cn.osc.gitee.util.GEGitRepositoryMapping
import cn.osc.gitee.util.GEHostedRepositoriesManager

internal class GEPRToolWindowTabViewModel(private val scope: CoroutineScope,
                                          private val repositoriesManager: GEHostedRepositoriesManager,
                                          private val accountManager: GEAccountManager,
                                          private val connectionManager: GERepositoryConnectionManager,
                                          private val settings: GiteePullRequestsProjectUISettings) {

  private val connectionState = MutableStateFlow<GERepositoryConnection?>(null).apply {
    scope.launch {
      collectLatest {
        if (it != null) {
          it.awaitClose()
          compareAndSet(it, null)
        }
      }
    }
  }

  private val singleRepoAndAccountState: StateFlow<Pair<GEGitRepositoryMapping, GiteeAccount>?> =
    combineState(scope, repositoriesManager.knownRepositoriesState, accountManager.accountsState) { repos, accounts ->
      repos.singleOrNull()?.let { repo ->
        accounts.singleOrNull { URIUtil.equalWithoutSchema(it.server.toURI(), repo.repository.serverPath.toURI()) }?.let {
          repo to it
        }
      }
    }

  val viewState: StateFlow<GEPRTabContentViewModel> = connectionState.mapStateScoped(scope) { scope, connection ->
    if (connection != null) {
      createConnectedVm(connection)
    }
    else {
      createNotConnectedVm(scope)
    }
  }

  private fun createNotConnectedVm(cs: CoroutineScope): GEPRTabContentViewModel.Selectors {
    val selectorVm = GERepositoryAndAccountSelectorViewModel(cs, repositoriesManager, accountManager, ::connect)

    settings.selectedRepoAndAccount?.let { (repo, account) ->
      with(selectorVm) {
        repoSelectionState.value = repo
        accountSelectionState.value = account
        submitSelection()
      }
    }

    cs.launch {
      singleRepoAndAccountState.collect {
        if (it != null) {
          with(selectorVm) {
            repoSelectionState.value = it.first
            accountSelectionState.value = it.second
            submitSelection()
          }
        }
      }
    }
    return GEPRTabContentViewModel.Selectors(selectorVm)
  }

  private suspend fun connect(repo: GEGitRepositoryMapping, account: GiteeAccount) {
    connectionState.value = connectionManager.connect(scope, repo, account)
    settings.selectedRepoAndAccount = repo to account
  }

  private fun createConnectedVm(connection: GERepositoryConnection) = GEPRTabContentViewModel.PullRequests(connection)

  fun canSelectDifferentRepoOrAccount(): Boolean {
    return viewState.value is GEPRTabContentViewModel.PullRequests && singleRepoAndAccountState.value == null
  }

  fun selectDifferentRepoOrAccount() {
    scope.launch {
      settings.selectedRepoAndAccount = null
      connectionState.value?.close()
    }
  }
}

internal sealed interface GEPRTabContentViewModel {
  class Selectors(val selectorVm: GERepositoryAndAccountSelectorViewModel) : GEPRTabContentViewModel
  class PullRequests(val connection: GERepositoryConnection) : GEPRTabContentViewModel
}
