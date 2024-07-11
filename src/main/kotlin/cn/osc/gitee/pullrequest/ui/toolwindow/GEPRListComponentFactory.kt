// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.ui.toolwindow

import com.intellij.collaboration.ui.codereview.list.*
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.ColorHexUtil
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBList
import icons.CollaborationToolsIcons
import cn.osc.gitee.api.data.GEActor
import cn.osc.gitee.api.data.GELabel
import cn.osc.gitee.api.data.GEUser
import cn.osc.gitee.api.data.pullrequest.*
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.pullrequest.action.GEPRActionKeys
import cn.osc.gitee.ui.avatars.GEAvatarIconsProvider
import cn.osc.gitee.ui.util.GEUIUtil
import javax.swing.ListModel

internal class GEPRListComponentFactory(private val listModel: ListModel<GEPullRequestShort>) {

  fun create(avatarIconsProvider: GEAvatarIconsProvider): JBList<GEPullRequestShort> {
    return ReviewListComponentFactory(listModel).create {
      presentPR(avatarIconsProvider, it)
    }.also {
      DataManager.registerDataProvider(it) { dataId ->
        if (GEPRActionKeys.SELECTED_PULL_REQUEST.`is`(dataId)) it.selectedValue else null
      }
      val groupId = "Gitee.PullRequest.ToolWindow.List.Popup"
      PopupHandler.installSelectionListPopup(it, ActionManager.getInstance().getAction(groupId) as ActionGroup, groupId)
      val shortcuts = CompositeShortcutSet(CommonShortcuts.ENTER, CommonShortcuts.DOUBLE_CLICK_1)
      EmptyAction.registerWithShortcutSet("Gitee.PullRequest.Show", shortcuts, it)
    }
  }

  private fun presentPR(avatarIconsProvider: GEAvatarIconsProvider, pr: GEPullRequestShort) =
    ReviewListItemPresentation.Simple(pr.title, "#" + pr.number, pr.createdAt,
                                      createUserPresentation(avatarIconsProvider, pr.author),
                                      tagGroup = NamedCollection.create(GiteeBundle.message("pull.request.labels.popup", pr.labels.size),
                                                                        pr.labels.map(::getLabelPresentation)),
                                      mergeableStatus = getMergeableStatus(pr.mergeable),
                                      state = getStateText(pr.state, pr.isDraft),
                                      userGroup1 = getAssigneesPresentation(avatarIconsProvider, pr.assignees),
                                      userGroup2 = getReviewersPresentation(avatarIconsProvider, pr.reviewRequests),
                                      commentsCounter = ReviewListItemPresentation.CommentsCounter(
                                        pr.unresolvedReviewThreadsCount,
                                        GiteeBundle.message("pull.request.unresolved.comments", pr.unresolvedReviewThreadsCount)
                                      ))

  private fun getLabelPresentation(label: GELabel) =
    TagPresentation.Simple(label.name, ColorHexUtil.fromHex(label.color))

  private fun getStateText(state: GEPullRequestState, isDraft: Boolean): @NlsSafe String? {
    if (state == GEPullRequestState.OPEN && !isDraft) return null
    return GEUIUtil.getPullRequestStateText(state, isDraft)
  }

  private fun getMergeableStatus(mergeableState: GEPullRequestMergeableState): ReviewListItemPresentation.Status? {
    if (mergeableState == GEPullRequestMergeableState.CONFLICTING) {
      return ReviewListItemPresentation.Status(CollaborationToolsIcons.Review.NonMergeable,
                                               GiteeBundle.message("pull.request.conflicts.merge.tooltip"))
    }

    return null
  }

  private fun getAssigneesPresentation(avatarIconsProvider: GEAvatarIconsProvider,
                                       assignees: List<GEUser>): NamedCollection<UserPresentation>? {
    return NamedCollection.create(GiteeBundle.message("pull.request.assignees.popup", assignees.size),
                                  assignees.map { user -> createUserPresentation(avatarIconsProvider, user) })
  }

  private fun getReviewersPresentation(avatarIconsProvider: GEAvatarIconsProvider,
                                       reviewRequests: List<GEPullRequestReviewRequest>): NamedCollection<UserPresentation>? {
    val reviewers = reviewRequests.mapNotNull { it.requestedReviewer }
    return NamedCollection.create(GiteeBundle.message("pull.request.reviewers.popup", reviewers.size),
                                  reviewers.map { reviewer -> createUserPresentation(avatarIconsProvider, reviewer) })
  }

  private fun createUserPresentation(avatarIconsProvider: GEAvatarIconsProvider, user: GEActor?): UserPresentation? {
    if (user == null) return null
    return UserPresentation.Simple(user.login, null, avatarIconsProvider.getIcon(user.avatarUrl, GEUIUtil.AVATAR_SIZE))
  }

  private fun createUserPresentation(avatarIconsProvider: GEAvatarIconsProvider, user: GEUser): UserPresentation =
    UserPresentation.Simple(user.login, user.name, avatarIconsProvider.getIcon(user.avatarUrl, GEUIUtil.AVATAR_SIZE))

  private fun createUserPresentation(avatarIconsProvider: GEAvatarIconsProvider, user: GEPullRequestRequestedReviewer): UserPresentation {
    return UserPresentation.Simple(user.shortName, user.name, avatarIconsProvider.getIcon(user.avatarUrl, GEUIUtil.AVATAR_SIZE))
  }
}