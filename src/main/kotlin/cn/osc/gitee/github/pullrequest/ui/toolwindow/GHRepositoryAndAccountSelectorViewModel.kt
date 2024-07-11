// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.github.pullrequest.ui.toolwindow

import com.intellij.collaboration.async.combineState
import git4idea.remote.hosting.ui.RepositoryAndAccountSelectorViewModelBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import cn.osc.gitee.github.authentication.accounts.GHAccountManager
import cn.osc.gitee.github.authentication.accounts.GithubAccount
import cn.osc.gitee.github.util.GHGitRepositoryMapping
import cn.osc.gitee.github.util.GHHostedRepositoriesManager

internal class GHRepositoryAndAccountSelectorViewModel(
  scope: CoroutineScope,
  repositoriesManager: GHHostedRepositoriesManager,
  accountManager: GHAccountManager,
  onSelected: suspend (GHGitRepositoryMapping, GithubAccount) -> Unit
) : RepositoryAndAccountSelectorViewModelBase<GHGitRepositoryMapping, GithubAccount>(
  scope,
  repositoriesManager,
  accountManager,
  onSelected) {

  val githubLoginAvailableState: StateFlow<Boolean> =
    combineState(scope, repoSelectionState, accountSelectionState, missingCredentialsState, ::isGithubLoginAvailable)

  private fun isGithubLoginAvailable(repo: GHGitRepositoryMapping?, account: GithubAccount?, credsMissing: Boolean?): Boolean {
    if (repo == null) return false
    return repo.repository.serverPath.isGithubDotCom && (account == null || credsMissing == true)
  }

  val gheLoginAvailableState: StateFlow<Boolean> =
    combineState(scope, repoSelectionState, accountSelectionState, missingCredentialsState, ::isGheLoginVisible)


  private fun isGheLoginVisible(repo: GHGitRepositoryMapping?, account: GithubAccount?, credsMissing: Boolean?): Boolean {
    if (repo == null) return false
    return !repo.repository.serverPath.isGithubDotCom && (account == null || credsMissing == true)
  }
}