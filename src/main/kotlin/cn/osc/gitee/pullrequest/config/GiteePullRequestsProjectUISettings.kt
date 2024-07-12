// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.config

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import git4idea.remote.hosting.knownRepositories
import cn.osc.gitee.api.GERepositoryCoordinates
import cn.osc.gitee.api.GERepositoryPath
import cn.osc.gitee.api.GiteeServerPath
import cn.osc.gitee.authentication.accounts.GEAccountSerializer
import cn.osc.gitee.authentication.accounts.GiteeAccount
import cn.osc.gitee.util.GEGitRepositoryMapping
import cn.osc.gitee.util.GEHostedRepositoriesManager

@Service(Service.Level.PROJECT)
@State(name = "GiteePullRequestsUISettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)], reportStatistic = false)
class GiteePullRequestsProjectUISettings(private val project: Project)
  : PersistentStateComponentWithModificationTracker<GiteePullRequestsProjectUISettings.SettingsState> {

  private var state: SettingsState = SettingsState()

  class SettingsState : BaseState() {
    var selectedUrlAndAccountId by property<UrlAndAccount?>(null) { it == null }
    var recentNewPullRequestHead by property<RepoCoordinatesHolder?>(null) { it == null }
  }

  var selectedRepoAndAccount: Pair<GEGitRepositoryMapping, GiteeAccount>?
    get() {
      val (url, accountId) = state.selectedUrlAndAccountId ?: return null
      val repo = project.service<GEHostedRepositoriesManager>().knownRepositories.find {
        it.remote.url == url
      } ?: return null
      val account = GEAccountSerializer.deserialize(accountId) ?: return null
      return repo to account
    }
    set(value) {
      state.selectedUrlAndAccountId = value?.let { (repo, account) ->
        UrlAndAccount(repo.remote.url, GEAccountSerializer.serialize(account))
      }
    }

  var recentNewPullRequestHead: GERepositoryCoordinates?
    get() = state.recentNewPullRequestHead?.let { GERepositoryCoordinates(it.server, GERepositoryPath(it.owner, it.repository)) }
    set(value) {
      state.recentNewPullRequestHead = value?.let { RepoCoordinatesHolder(it) }
    }

  override fun getStateModificationCount() = state.modificationCount
  override fun getState() = state
  override fun loadState(state: SettingsState) {
    this.state = state
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project) = project.service<GiteePullRequestsProjectUISettings>()

    class UrlAndAccount private constructor() {

      @Suppress("MemberVisibilityCanBePrivate")
      var url: String = ""
      @Suppress("MemberVisibilityCanBePrivate")
      var accountId: String = ""

      constructor(url: String, accountId: String) : this() {
        this.url = url
        this.accountId = accountId
      }

      operator fun component1() = url
      operator fun component2() = accountId
    }

    class RepoCoordinatesHolder private constructor() {

      var server: GiteeServerPath = GiteeServerPath.DEFAULT_SERVER
      var owner: String = ""
      var repository: String = ""

      constructor(coordinates: GERepositoryCoordinates): this() {
        server = coordinates.serverPath
        owner = coordinates.repositoryPath.owner
        repository = coordinates.repositoryPath.repository
      }
    }
  }
}