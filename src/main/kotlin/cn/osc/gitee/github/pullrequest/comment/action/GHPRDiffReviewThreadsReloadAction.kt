// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.comment.action

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.actionSystem.AnActionEvent
import cn.osc.gitee.github.i18n.GithubBundle
import cn.osc.gitee.github.pullrequest.comment.GHPRDiffReviewSupport

class GHPRDiffReviewThreadsReloadAction
  : RefreshAction({ GithubBundle.message("pull.request.review.refresh.data.task") },
                  { GithubBundle.message("pull.request.review.refresh.data.task.description") },
                  AllIcons.Actions.Refresh) {

  override fun update(e: AnActionEvent) {
    val reviewSupport = e.getData(GHPRDiffReviewSupport.DATA_KEY)
    e.presentation.isVisible = reviewSupport != null
    e.presentation.isEnabled = reviewSupport?.isLoadingReviewData?.not() ?: false
  }

  override fun actionPerformed(e: AnActionEvent) {
    e.getRequiredData(GHPRDiffReviewSupport.DATA_KEY).reloadReviewData()
  }
}