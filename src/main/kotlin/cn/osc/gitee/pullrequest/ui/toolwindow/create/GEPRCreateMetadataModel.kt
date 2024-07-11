// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.ui.toolwindow.create

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.EventDispatcher
import cn.osc.gitee.api.data.GELabel
import cn.osc.gitee.api.data.GEUser
import cn.osc.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import cn.osc.gitee.pullrequest.data.service.GEPRRepositoryDataService
import com.intellij.collaboration.ui.SimpleEventListener
import cn.osc.gitee.pullrequest.ui.details.GEPRMetadataModelBase
import com.intellij.collaboration.util.CollectionDelta
import java.util.concurrent.CompletableFuture
import kotlin.properties.Delegates.observable

class GEPRCreateMetadataModel(repositoryDataService: GEPRRepositoryDataService,
                              private val currentUser: GEUser)
  : GEPRMetadataModelBase(repositoryDataService) {

  private val eventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

  override var assignees: List<GEUser> by observable(emptyList()) { _, _, _ ->
    eventDispatcher.multicaster.eventOccurred()
  }
  override var reviewers: List<GEPullRequestRequestedReviewer> by observable(emptyList()) { _, _, _ ->
    eventDispatcher.multicaster.eventOccurred()
  }
  override var labels: List<GELabel> by observable(emptyList()) { _, _, _ ->
    eventDispatcher.multicaster.eventOccurred()
  }

  override val isEditingAllowed = true

  override fun getAuthor() = currentUser

  override fun adjustAssignees(indicator: ProgressIndicator, delta: CollectionDelta<GEUser>): CompletableFuture<Unit> {
    assignees = ArrayList(delta.newCollection)
    return CompletableFuture.completedFuture(Unit)
  }

  override fun adjustReviewers(indicator: ProgressIndicator,
                               delta: CollectionDelta<GEPullRequestRequestedReviewer>): CompletableFuture<Unit> {
    reviewers = ArrayList(delta.newCollection)
    return CompletableFuture.completedFuture(Unit)
  }

  override fun adjustLabels(indicator: ProgressIndicator, delta: CollectionDelta<GELabel>): CompletableFuture<Unit> {
    labels = ArrayList(delta.newCollection)
    return CompletableFuture.completedFuture(Unit)
  }

  override fun addAndInvokeChangesListener(listener: () -> Unit) = SimpleEventListener.addAndInvokeListener(eventDispatcher, listener)
}