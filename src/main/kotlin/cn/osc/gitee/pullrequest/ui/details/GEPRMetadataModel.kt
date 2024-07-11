// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.ui.details

import com.intellij.openapi.progress.ProgressIndicator
import cn.osc.gitee.api.data.GELabel
import cn.osc.gitee.api.data.GEUser
import cn.osc.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import com.intellij.collaboration.util.CollectionDelta
import java.util.concurrent.CompletableFuture

interface GEPRMetadataModel {
  val assignees: List<GEUser>
  val reviewers: List<GEPullRequestRequestedReviewer>
  val labels: List<GELabel>

  val isEditingAllowed: Boolean

  fun loadPotentialReviewers(): CompletableFuture<List<GEPullRequestRequestedReviewer>>
  fun adjustReviewers(indicator: ProgressIndicator, delta: CollectionDelta<GEPullRequestRequestedReviewer>): CompletableFuture<Unit>

  fun loadPotentialAssignees(): CompletableFuture<List<GEUser>>
  fun adjustAssignees(indicator: ProgressIndicator, delta: CollectionDelta<GEUser>): CompletableFuture<Unit>

  fun loadAssignableLabels(): CompletableFuture<List<GELabel>>
  fun adjustLabels(indicator: ProgressIndicator, delta: CollectionDelta<GELabel>): CompletableFuture<Unit>

  fun addAndInvokeChangesListener(listener: () -> Unit)
}