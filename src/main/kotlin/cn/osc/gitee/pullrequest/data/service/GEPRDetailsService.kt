// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data.service

import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.annotations.CalledInAny
import cn.osc.gitee.api.data.GELabel
import cn.osc.gitee.api.data.GEUser
import cn.osc.gitee.api.data.pullrequest.GEPullRequest
import cn.osc.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import cn.osc.gitee.pullrequest.data.GEPRIdentifier
import com.intellij.collaboration.util.CollectionDelta
import java.util.concurrent.CompletableFuture

interface GEPRDetailsService {

  @CalledInAny
  fun loadDetails(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier): CompletableFuture<GEPullRequest>

  @CalledInAny
  fun updateDetails(indicator: ProgressIndicator, pullRequestId: GEPRIdentifier, title: String?, description: String?)
    : CompletableFuture<GEPullRequest>

  @CalledInAny
  fun adjustReviewers(indicator: ProgressIndicator, pullRequestId: GEPRIdentifier, delta: CollectionDelta<GEPullRequestRequestedReviewer>)
    : CompletableFuture<Unit>

  @CalledInAny
  fun adjustAssignees(indicator: ProgressIndicator, pullRequestId: GEPRIdentifier, delta: CollectionDelta<GEUser>)
    : CompletableFuture<Unit>

  @CalledInAny
  fun adjustLabels(indicator: ProgressIndicator, pullRequestId: GEPRIdentifier, delta: CollectionDelta<GELabel>)
    : CompletableFuture<Unit>
}