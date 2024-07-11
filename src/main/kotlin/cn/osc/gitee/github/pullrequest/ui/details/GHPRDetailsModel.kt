// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.ui.details

import cn.osc.gitee.github.api.data.pullrequest.GHPullRequestState

interface GHPRDetailsModel {

  val number: String
  val title: String
  val description: String
  val state: GHPullRequestState
  val isDraft: Boolean

  fun addAndInvokeDetailsChangedListener(listener: () -> Unit)
}