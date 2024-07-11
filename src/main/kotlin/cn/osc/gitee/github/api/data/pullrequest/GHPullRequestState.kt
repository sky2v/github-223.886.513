// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.api.data.pullrequest

import cn.osc.gitee.github.api.data.GithubIssueState

enum class GHPullRequestState {
  CLOSED, MERGED, OPEN;

  fun asIssueState(): GithubIssueState = if(this == CLOSED || this == MERGED) GithubIssueState.closed else GithubIssueState.open
}