// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.comment.ui

import cn.osc.gitee.github.api.data.pullrequest.GHPullRequestPendingReview
import com.intellij.collaboration.ui.SimpleEventListener

interface GHPRReviewProcessModel {
  val pendingReview: GHPullRequestPendingReview?
  val isActual: Boolean

  fun populatePendingReviewData(review: GHPullRequestPendingReview?)
  fun clearPendingReviewData()

  fun addAndInvokeChangesListener(listener: SimpleEventListener)
  fun removeChangesListener(listener: SimpleEventListener)
}