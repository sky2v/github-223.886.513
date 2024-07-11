// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data.service

import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.annotations.CalledInAny
import cn.osc.gitee.api.data.GEComment
import cn.osc.gitee.api.data.GiteeIssueCommentWithHtml
import cn.osc.gitee.pullrequest.data.GEPRIdentifier
import java.util.concurrent.CompletableFuture

interface GEPRCommentService {

  @CalledInAny
  fun addComment(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier, body: String)
    : CompletableFuture<GiteeIssueCommentWithHtml>

  @CalledInAny
  fun updateComment(progressIndicator: ProgressIndicator, commentId: String, text: String): CompletableFuture<GEComment>

  @CalledInAny
  fun deleteComment(progressIndicator: ProgressIndicator, commentId: String): CompletableFuture<out Any?>
}
