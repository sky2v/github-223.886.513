// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data.service

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import git4idea.GitRemoteBranch
import org.jetbrains.annotations.CalledInAny
import cn.osc.gitee.api.data.pullrequest.GEPullRequestShort
import cn.osc.gitee.pullrequest.data.GEPRIdentifier
import cn.osc.gitee.util.GEGitRepositoryMapping
import java.util.concurrent.CompletableFuture

interface GEPRCreationService {

  @CalledInAny
  fun createPullRequest(progressIndicator: ProgressIndicator,
                        baseBranch: GitRemoteBranch,
                        headRepo: GEGitRepositoryMapping,
                        headBranch: GitRemoteBranch,
                        title: String,
                        description: String,
                        draft: Boolean): CompletableFuture<GEPullRequestShort>

  @RequiresBackgroundThread
  fun findPullRequest(progressIndicator: ProgressIndicator,
                      baseBranch: GitRemoteBranch,
                      headRepo: GEGitRepositoryMapping,
                      headBranch: GitRemoteBranch): GEPRIdentifier?
}