// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data.provider

import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.concurrency.annotations.RequiresEdt
import cn.osc.gitee.api.data.GELabel
import cn.osc.gitee.api.data.GEUser
import cn.osc.gitee.api.data.pullrequest.GEPullRequest
import cn.osc.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import com.intellij.collaboration.util.CollectionDelta
import java.util.concurrent.CompletableFuture

interface GEPRDetailsDataProvider {

  val loadedDetails: GEPullRequest?

  @RequiresEdt
  fun loadDetails(): CompletableFuture<GEPullRequest>

  @RequiresEdt
  fun reloadDetails()

  @RequiresEdt
  fun addDetailsReloadListener(disposable: Disposable, listener: () -> Unit)

  @RequiresEdt
  fun loadDetails(disposable: Disposable, consumer: (CompletableFuture<GEPullRequest>) -> Unit) {
    addDetailsReloadListener(disposable) {
      consumer(loadDetails())
    }
    consumer(loadDetails())
  }

  @RequiresEdt
  fun addDetailsLoadedListener(disposable: Disposable, listener: () -> Unit)

  @RequiresEdt
  fun updateDetails(indicator: ProgressIndicator, title: String? = null, description: String? = null): CompletableFuture<GEPullRequest>

  @RequiresEdt
  fun adjustReviewers(indicator: ProgressIndicator, delta: CollectionDelta<GEPullRequestRequestedReviewer>)
    : CompletableFuture<Unit>

  @RequiresEdt
  fun adjustAssignees(indicator: ProgressIndicator, delta: CollectionDelta<GEUser>)
    : CompletableFuture<Unit>

  @RequiresEdt
  fun adjustLabels(indicator: ProgressIndicator, delta: CollectionDelta<GELabel>)
    : CompletableFuture<Unit>
}