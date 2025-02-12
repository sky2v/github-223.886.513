// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data.provider

import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.annotations.CalledInAny
import cn.osc.gitee.pullrequest.data.GEPRMergeabilityState
import java.util.concurrent.CompletableFuture

interface GEPRStateDataProvider {

  @RequiresEdt
  fun loadMergeabilityState(): CompletableFuture<GEPRMergeabilityState>

  @RequiresEdt
  fun reloadMergeabilityState()

  @RequiresEdt
  fun addMergeabilityStateListener(disposable: Disposable, listener: () -> Unit)

  @RequiresEdt
  fun loadMergeabilityState(disposable: Disposable, consumer: (CompletableFuture<GEPRMergeabilityState>) -> Unit) {
    addMergeabilityStateListener(disposable) {
      consumer(loadMergeabilityState())
    }
    consumer(loadMergeabilityState())
  }


  @CalledInAny
  fun close(progressIndicator: ProgressIndicator): CompletableFuture<Unit>

  @CalledInAny
  fun reopen(progressIndicator: ProgressIndicator): CompletableFuture<Unit>

  @RequiresEdt
  fun markReadyForReview(progressIndicator: ProgressIndicator): CompletableFuture<Unit>

  @CalledInAny
  fun merge(progressIndicator: ProgressIndicator, commitMessage: Pair<String, String>, currentHeadRef: String): CompletableFuture<Unit>

  @CalledInAny
  fun rebaseMerge(progressIndicator: ProgressIndicator, currentHeadRef: String): CompletableFuture<Unit>

  @CalledInAny
  fun squashMerge(progressIndicator: ProgressIndicator,
                  commitMessage: Pair<String, String>,
                  currentHeadRef: String): CompletableFuture<Unit>
}