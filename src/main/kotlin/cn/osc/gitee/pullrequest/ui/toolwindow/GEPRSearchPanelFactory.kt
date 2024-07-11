// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.pullrequest.ui.toolwindow

import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil.PopupItemPresentation
import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil.showAsyncChooserPopup
import com.intellij.collaboration.ui.codereview.list.search.DropDownComponentFactory
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.jetbrains.annotations.Nls
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.ui.avatars.GEAvatarIconsProvider
import cn.osc.gitee.ui.util.GEUIUtil
import javax.swing.JComponent

internal class GEPRSearchPanelFactory(vm: GEPRSearchPanelViewModel, private val avatarIconsProvider: GEAvatarIconsProvider) :
  ReviewListSearchPanelFactory<GEPRListSearchValue, GEPRListQuickFilter, GEPRSearchPanelViewModel>(vm) {

  override fun getShortText(searchValue: GEPRListSearchValue): @Nls String = with(searchValue) {
    @Suppress("HardCodedStringLiteral")
    StringBuilder().apply {
      if (searchQuery != null) append(""""$searchQuery"""").append(" ")
      if (state != null) append("""state:"${getShortText(state)}"""").append(" ")
      if (label != null) append("label:$label").append(" ")
      if (assignee != null) append("assignee:$assignee").append(" ")
      if (reviewState != null) append("""reviewState:"${getShortText(reviewState)}"""").append(" ")
      if (author != null) append("author:$author").append(" ")
    }.toString()
  }

  override fun createFilters(viewScope: CoroutineScope): List<JComponent> = listOf(
    DropDownComponentFactory(vm.stateFilterState)
      .create(viewScope, GiteeBundle.message("pull.request.list.filter.state"),
              GEPRListSearchValue.State.values().asList(),
              ::getShortText),
    DropDownComponentFactory(vm.authorFilterState)
      .create(viewScope, GiteeBundle.message("pull.request.list.filter.author")) { point, popupState ->
        showAsyncChooserPopup(point, popupState, { vm.getAuthors() }) {
          PopupItemPresentation.Simple(it.shortName, avatarIconsProvider.getIcon(it.avatarUrl, GEUIUtil.AVATAR_SIZE), it.name)
        }?.login
      },
    DropDownComponentFactory(vm.labelFilterState)
      .create(viewScope, GiteeBundle.message("pull.request.list.filter.label")) { point, popupState ->
        showAsyncChooserPopup(point, popupState, { vm.getLabels() }) {
          PopupItemPresentation.Simple(it.name)
        }?.name
      },
    DropDownComponentFactory(vm.assigneeFilterState)
      .create(viewScope, GiteeBundle.message("pull.request.list.filter.assignee")) { point, popupState ->
        showAsyncChooserPopup(point, popupState, { vm.getAssignees() }) {
          PopupItemPresentation.Simple(it.shortName, avatarIconsProvider.getIcon(it.avatarUrl, GEUIUtil.AVATAR_SIZE), it.name)
        }?.login
      },
    DropDownComponentFactory(vm.reviewFilterState)
      .create(viewScope, GiteeBundle.message("pull.request.list.filter.review"),
              GEPRListSearchValue.ReviewState.values().asList(),
              ::getShortText) {
        PopupItemPresentation.Simple(getFullText(it))
      }
  )

  override fun GEPRListQuickFilter.getQuickFilterTitle(): String = when (this) {
    is GEPRListQuickFilter.Open -> GiteeBundle.message("pull.request.list.filter.quick.open")
    is GEPRListQuickFilter.YourPullRequests -> GiteeBundle.message("pull.request.list.filter.quick.yours")
    is GEPRListQuickFilter.AssignedToYou -> GiteeBundle.message("pull.request.list.filter.quick.assigned")
  }

  companion object {
    private fun getShortText(stateFilterValue: GEPRListSearchValue.State): @Nls String = when (stateFilterValue) {
      GEPRListSearchValue.State.OPEN -> GiteeBundle.message("pull.request.list.filter.state.open")
      GEPRListSearchValue.State.CLOSED -> GiteeBundle.message("pull.request.list.filter.state.closed")
      GEPRListSearchValue.State.MERGED -> GiteeBundle.message("pull.request.list.filter.state.merged")
    }

    private fun getShortText(reviewStateFilterValue: GEPRListSearchValue.ReviewState): @Nls String = when (reviewStateFilterValue) {
      GEPRListSearchValue.ReviewState.NO_REVIEW -> GiteeBundle.message("pull.request.list.filter.review.no.short")
      GEPRListSearchValue.ReviewState.REQUIRED -> GiteeBundle.message("pull.request.list.filter.review.required.short")
      GEPRListSearchValue.ReviewState.APPROVED -> GiteeBundle.message("pull.request.list.filter.review.approved.short")
      GEPRListSearchValue.ReviewState.CHANGES_REQUESTED -> GiteeBundle.message("pull.request.list.filter.review.change.requested.short")
      GEPRListSearchValue.ReviewState.REVIEWED_BY_ME -> GiteeBundle.message("pull.request.list.filter.review.reviewed.short")
      GEPRListSearchValue.ReviewState.NOT_REVIEWED_BY_ME -> GiteeBundle.message("pull.request.list.filter.review.not.short")
      GEPRListSearchValue.ReviewState.AWAITING_REVIEW -> GiteeBundle.message("pull.request.list.filter.review.awaiting.short")
    }

    private fun getFullText(reviewStateFilterValue: GEPRListSearchValue.ReviewState): @Nls String = when (reviewStateFilterValue) {
      GEPRListSearchValue.ReviewState.NO_REVIEW -> GiteeBundle.message("pull.request.list.filter.review.no.full")
      GEPRListSearchValue.ReviewState.REQUIRED -> GiteeBundle.message("pull.request.list.filter.review.required.full")
      GEPRListSearchValue.ReviewState.APPROVED -> GiteeBundle.message("pull.request.list.filter.review.approved.full")
      GEPRListSearchValue.ReviewState.CHANGES_REQUESTED -> GiteeBundle.message("pull.request.list.filter.review.change.requested.full")
      GEPRListSearchValue.ReviewState.REVIEWED_BY_ME -> GiteeBundle.message("pull.request.list.filter.review.reviewed.full")
      GEPRListSearchValue.ReviewState.NOT_REVIEWED_BY_ME -> GiteeBundle.message("pull.request.list.filter.review.not.full")
      GEPRListSearchValue.ReviewState.AWAITING_REVIEW -> GiteeBundle.message("pull.request.list.filter.review.awaiting.full")
    }
  }
}