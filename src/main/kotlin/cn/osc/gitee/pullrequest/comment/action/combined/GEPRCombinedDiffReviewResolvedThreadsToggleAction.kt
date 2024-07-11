// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.pullrequest.comment.action.combined

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.vcs.changes.actions.diff.CombinedDiffPreviewModel
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.pullrequest.comment.GEPRDiffReviewSupport

internal class GEPRCombinedDiffReviewResolvedThreadsToggleAction(private val model: CombinedDiffPreviewModel) :
  ToggleAction({ GiteeBundle.message("pull.request.review.show.resolved.threads") },
               { GiteeBundle.message("pull.request.review.show.resolved.threads.description") }, null) {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  override fun update(e: AnActionEvent) {
    super.update(e)

    val reviewSupports =
      model.getLoadedRequests().mapNotNull { it.getUserData(GEPRDiffReviewSupport.KEY) }

    e.presentation.isVisible = reviewSupports.isNotEmpty()
    e.presentation.isEnabled = reviewSupports.all { it.showReviewThreads }
  }

  override fun isSelected(e: AnActionEvent): Boolean =
    e.getData(GEPRDiffReviewSupport.DATA_KEY)?.showResolvedReviewThreads ?: false

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    model.getLoadedRequests()
      .forEach { request -> request.getUserData(GEPRDiffReviewSupport.KEY)?.showResolvedReviewThreads = state }
  }

}
