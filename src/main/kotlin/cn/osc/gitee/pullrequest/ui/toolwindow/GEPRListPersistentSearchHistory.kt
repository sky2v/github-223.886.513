// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.pullrequest.ui.toolwindow

import com.intellij.openapi.components.*
import kotlinx.serialization.Serializable

@Service(Service.Level.PROJECT)
@State(name = "GitHubPullRequestSearchHistory", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)], reportStatistic = false)
internal class GEPRListPersistentSearchHistory : SerializablePersistentStateComponent<GEPRListPersistentSearchHistory.HistoryState>(
  HistoryState()) {

  @Serializable
  data class HistoryState(val history: List<GEPRListSearchValue> = emptyList(), val lastFilter: GEPRListSearchValue? = null)

  var lastFilter: GEPRListSearchValue?
    get() = state.lastFilter
    set(value) {
      updateState {
        it.copy(lastFilter = value)
      }
    }

  var history: List<GEPRListSearchValue>
    get() = state.history.toList()
    set(value) {
      updateState {
        it.copy(history = value)
      }
    }
}
