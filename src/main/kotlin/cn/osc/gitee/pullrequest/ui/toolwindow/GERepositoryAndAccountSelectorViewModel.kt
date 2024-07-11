// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.pullrequest.ui.toolwindow

import com.intellij.collaboration.async.combineState
import git4idea.remote.hosting.ui.RepositoryAndAccountSelectorViewModelBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import cn.osc.gitee.authentication.accounts.GEAccountManager
import cn.osc.gitee.authentication.accounts.GiteeAccount
import cn.osc.gitee.util.GEGitRepositoryMapping
import cn.osc.gitee.util.GEHostedRepositoriesManager

internal class GERepositoryAndAccountSelectorViewModel(
  scope: CoroutineScope,
  repositoriesManager: GEHostedRepositoriesManager,
  accountManager: GEAccountManager,
  onSelected: suspend (GEGitRepositoryMapping, GiteeAccount) -> Unit
) : RepositoryAndAccountSelectorViewModelBase<GEGitRepositoryMapping, GiteeAccount>(
  scope,
  repositoriesManager,
  accountManager,
  onSelected) {

  val githubLoginAvailableState: StateFlow<Boolean> =
    combineState(scope, repoSelectionState, accountSelectionState, missingCredentialsState, ::isGithubLoginAvailable)

  private fun isGithubLoginAvailable(repo: GEGitRepositoryMapping?, account: GiteeAccount?, credsMissing: Boolean?): Boolean {
    if (repo == null) return false
    return repo.repository.serverPath.isGithubDotCom && (account == null || credsMissing == true)
  }

  val gheLoginAvailableState: StateFlow<Boolean> =
    combineState(scope, repoSelectionState, accountSelectionState, missingCredentialsState, ::isGheLoginVisible)


  private fun isGheLoginVisible(repo: GEGitRepositoryMapping?, account: GiteeAccount?, credsMissing: Boolean?): Boolean {
    if (repo == null) return false
    return !repo.repository.serverPath.isGithubDotCom && (account == null || credsMissing == true)
  }
}