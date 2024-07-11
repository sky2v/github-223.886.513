// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.pullrequest.ui.toolwindow

import com.intellij.collaboration.ui.codereview.list.search.PersistingReviewListSearchHistoryModel

internal class GEPRSearchHistoryModel(private val persistentHistoryComponent: GEPRListPersistentSearchHistory)
  : PersistingReviewListSearchHistoryModel<GEPRListSearchValue>() {

  override var lastFilter: GEPRListSearchValue?
    get() = persistentHistoryComponent.lastFilter
    set(value) {
      persistentHistoryComponent.lastFilter = value
    }

  override var persistentHistory: List<GEPRListSearchValue>
    get() = persistentHistoryComponent.history
    set(value) {
      persistentHistoryComponent.history = value
    }
}