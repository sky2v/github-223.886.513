// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data.provider

import com.intellij.collaboration.async.CompletableFutureUtil.completionOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.collaboration.util.CollectionDelta
import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.EventDispatcher
import com.intellij.util.messages.MessageBus
import cn.osc.gitee.api.data.GELabel
import cn.osc.gitee.api.data.GEUser
import cn.osc.gitee.api.data.pullrequest.GEPullRequest
import cn.osc.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import cn.osc.gitee.pullrequest.data.GEPRIdentifier
import cn.osc.gitee.pullrequest.data.service.GEPRDetailsService
import cn.osc.gitee.util.LazyCancellableBackgroundProcessValue
import java.util.concurrent.CompletableFuture

class GEPRDetailsDataProviderImpl(private val detailsService: GEPRDetailsService,
                                  private val pullRequestId: GEPRIdentifier,
                                  private val messageBus: MessageBus)
  : GEPRDetailsDataProvider, Disposable {

  private val detailsLoadedEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

  @Volatile
  override var loadedDetails: GEPullRequest? = null
    private set

  private val detailsRequestValue = LazyCancellableBackgroundProcessValue.create { indicator ->
    detailsService.loadDetails(indicator, pullRequestId).successOnEdt {
      loadedDetails = it
      detailsLoadedEventDispatcher.multicaster.eventOccurred()
      it
    }
  }

  override fun loadDetails(): CompletableFuture<GEPullRequest> = detailsRequestValue.value

  override fun reloadDetails() = detailsRequestValue.drop()

  override fun updateDetails(indicator: ProgressIndicator, title: String?, description: String?): CompletableFuture<GEPullRequest> {
    val future = detailsService.updateDetails(indicator, pullRequestId, title, description).completionOnEdt {
      messageBus.syncPublisher(GEPRDataOperationsListener.TOPIC).onMetadataChanged()
    }
    detailsRequestValue.overrideProcess(future.successOnEdt {
      loadedDetails = it
      detailsLoadedEventDispatcher.multicaster.eventOccurred()
      it
    })
    return future
  }

  override fun adjustReviewers(indicator: ProgressIndicator,
                               delta: CollectionDelta<GEPullRequestRequestedReviewer>): CompletableFuture<Unit> {
    return detailsService.adjustReviewers(indicator, pullRequestId, delta).notify()
  }

  override fun adjustAssignees(indicator: ProgressIndicator, delta: CollectionDelta<GEUser>): CompletableFuture<Unit> {
    return detailsService.adjustAssignees(indicator, pullRequestId, delta).notify()
  }

  override fun adjustLabels(indicator: ProgressIndicator, delta: CollectionDelta<GELabel>): CompletableFuture<Unit> {
    return detailsService.adjustLabels(indicator, pullRequestId, delta).notify()
  }

  override fun addDetailsReloadListener(disposable: Disposable, listener: () -> Unit) =
    detailsRequestValue.addDropEventListener(disposable, listener)

  override fun addDetailsLoadedListener(disposable: Disposable, listener: () -> Unit) =
    SimpleEventListener.addDisposableListener(detailsLoadedEventDispatcher, disposable, listener)

  private fun <T> CompletableFuture<T>.notify(): CompletableFuture<T> =
    completionOnEdt {
      detailsRequestValue.drop()
      messageBus.syncPublisher(GEPRDataOperationsListener.TOPIC).onMetadataChanged()
    }

  override fun dispose() {
    detailsRequestValue.drop()
  }
}