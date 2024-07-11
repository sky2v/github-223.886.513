// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.api.data.request;

import org.jetbrains.annotations.NotNull;
import cn.osc.gitee.github.api.data.GithubPullRequestMergeMethod;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class GithubPullRequestMergeRebaseRequest {
  @NotNull private final String sha;
  @NotNull private final GithubPullRequestMergeMethod mergeMethod;

  public GithubPullRequestMergeRebaseRequest(@NotNull String sha) {
    this.sha = sha;
    this.mergeMethod = GithubPullRequestMergeMethod.rebase;
  }
}
