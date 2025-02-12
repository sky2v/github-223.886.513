// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.pullrequest.comment.action.combined

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vcs.changes.actions.diff.CombinedDiffPreviewModel
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.pullrequest.comment.GEPRDiffReviewSupport

internal class GEPRCombinedDiffReviewThreadsReloadAction(private val model: CombinedDiffPreviewModel)
  : RefreshAction({ GiteeBundle.message("pull.request.review.refresh.data.task") },
                  { GiteeBundle.message("pull.request.review.refresh.data.task.description") },
                  AllIcons.Actions.Refresh) {

  override fun update(e: AnActionEvent) {
    val reviewSupports =
      model.getLoadedRequests().mapNotNull { it.getUserData(GEPRDiffReviewSupport.KEY) }

    e.presentation.isVisible = reviewSupports.isNotEmpty()
    e.presentation.isEnabled = reviewSupports.all { it.isLoadingReviewData.not() }
  }

  override fun actionPerformed(e: AnActionEvent) {
    model.getLoadedRequests().forEach { request -> request.getUserData(GEPRDiffReviewSupport.KEY)?.reloadReviewData() }
  }
}
