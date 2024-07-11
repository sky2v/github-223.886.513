// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.pullrequest.ui.toolwindow

import com.intellij.collaboration.ui.codereview.list.search.ReviewListQuickFilter
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelViewModelBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import cn.osc.gitee.api.data.GELabel
import cn.osc.gitee.api.data.GEUser
import cn.osc.gitee.pullrequest.data.service.GEPRRepositoryDataService
import cn.osc.gitee.pullrequest.ui.toolwindow.GEPRListQuickFilter.*

internal class GEPRSearchPanelViewModel(
  scope: CoroutineScope,
  private val repositoryDataService: GEPRRepositoryDataService,
  historyViewModel: GEPRSearchHistoryModel,
  currentUser: GEUser
) :
  ReviewListSearchPanelViewModelBase<GEPRListSearchValue, GEPRListQuickFilter>(
    scope, historyViewModel,
    emptySearch = GEPRListSearchValue.EMPTY,
    defaultQuickFilter = Open(currentUser)
  ) {

  override fun GEPRListSearchValue.withQuery(query: String?) = copy(searchQuery = query)

  override val quickFilters: List<GEPRListQuickFilter> = listOf(
    Open(currentUser),
    YourPullRequests(currentUser),
    AssignedToYou(currentUser)
  )

  val stateFilterState = searchState.partialState(GEPRListSearchValue::state) {
    copy(state = it)
  }

  val authorFilterState = searchState.partialState(GEPRListSearchValue::author) {
    copy(author = it)
  }

  val labelFilterState = searchState.partialState(GEPRListSearchValue::label) {
    copy(label = it)
  }

  val assigneeFilterState = searchState.partialState(GEPRListSearchValue::assignee) {
    copy(assignee = it)
  }


  val reviewFilterState = searchState.partialState(GEPRListSearchValue::reviewState) {
    copy(reviewState = it)
  }

  suspend fun getAuthors(): List<GEUser> = repositoryDataService.collaborators.await()

  suspend fun getAssignees(): List<GEUser> = repositoryDataService.issuesAssignees.await()
  suspend fun getLabels(): List<GELabel> = repositoryDataService.labels.await()
}

internal sealed class GEPRListQuickFilter(user: GEUser) : ReviewListQuickFilter<GEPRListSearchValue> {
  protected val userLogin = user.login

  data class Open(val user: GEUser) : GEPRListQuickFilter(user) {
    override val filter = GEPRListSearchValue(state = GEPRListSearchValue.State.OPEN)
  }

  data class YourPullRequests(val user: GEUser) : GEPRListQuickFilter(user) {
    override val filter = GEPRListSearchValue(state = GEPRListSearchValue.State.OPEN, author = userLogin)
  }

  data class AssignedToYou(val user: GEUser) : GEPRListQuickFilter(user) {
    override val filter = GEPRListSearchValue(state = GEPRListSearchValue.State.OPEN, assignee = userLogin)
  }
}