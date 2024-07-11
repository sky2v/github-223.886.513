// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.comment.ui

import cn.osc.gitee.github.api.data.GHCommitHash
import cn.osc.gitee.github.api.data.pullrequest.GHPullRequestReviewCommentState
import cn.osc.gitee.github.api.data.pullrequest.GHPullRequestReviewThread
import java.util.*
import javax.swing.ListModel

interface GHPRReviewThreadModel : ListModel<GHPRReviewCommentModel> {
  val id: String
  val createdAt: Date
  val state: GHPullRequestReviewCommentState
  val isResolved: Boolean
  val isOutdated: Boolean
  val commit: GHCommitHash?
  val filePath: String
  val diffHunk: String
  val line: Int
  val startLine: Int?

  fun update(thread: GHPullRequestReviewThread)
  fun addComment(comment: GHPRReviewCommentModel)

  fun addAndInvokeStateChangeListener(listener: () -> Unit)
}
