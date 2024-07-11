// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.data.service

import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.annotations.CalledInAny
import cn.osc.gitee.github.api.data.GHLabel
import cn.osc.gitee.github.api.data.GHUser
import cn.osc.gitee.github.api.data.pullrequest.GHPullRequest
import cn.osc.gitee.github.api.data.pullrequest.GHPullRequestRequestedReviewer
import cn.osc.gitee.github.pullrequest.data.GHPRIdentifier
import com.intellij.collaboration.util.CollectionDelta
import java.util.concurrent.CompletableFuture

interface GHPRDetailsService {

  @CalledInAny
  fun loadDetails(progressIndicator: ProgressIndicator, pullRequestId: GHPRIdentifier): CompletableFuture<GHPullRequest>

  @CalledInAny
  fun updateDetails(indicator: ProgressIndicator, pullRequestId: GHPRIdentifier, title: String?, description: String?)
    : CompletableFuture<GHPullRequest>

  @CalledInAny
  fun adjustReviewers(indicator: ProgressIndicator, pullRequestId: GHPRIdentifier, delta: CollectionDelta<GHPullRequestRequestedReviewer>)
    : CompletableFuture<Unit>

  @CalledInAny
  fun adjustAssignees(indicator: ProgressIndicator, pullRequestId: GHPRIdentifier, delta: CollectionDelta<GHUser>)
    : CompletableFuture<Unit>

  @CalledInAny
  fun adjustLabels(indicator: ProgressIndicator, pullRequestId: GHPRIdentifier, delta: CollectionDelta<GHLabel>)
    : CompletableFuture<Unit>
}