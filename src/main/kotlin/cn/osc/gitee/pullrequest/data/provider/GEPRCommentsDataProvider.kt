// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data.provider

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.concurrency.annotations.RequiresEdt
import cn.osc.gitee.api.data.GiteeIssueCommentWithHtml
import java.util.concurrent.CompletableFuture

interface GEPRCommentsDataProvider {

  @RequiresEdt
  fun addComment(progressIndicator: ProgressIndicator, body: String)
    : CompletableFuture<GiteeIssueCommentWithHtml>

  @RequiresEdt
  fun updateComment(progressIndicator: ProgressIndicator, commentId: String, text: String): CompletableFuture<@NlsSafe String>

  @RequiresEdt
  fun deleteComment(progressIndicator: ProgressIndicator, commentId: String): CompletableFuture<out Any?>

}