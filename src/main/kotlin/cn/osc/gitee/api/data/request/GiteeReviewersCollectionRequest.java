// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data.request;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class GiteeReviewersCollectionRequest {
  @NotNull private final Collection<String> reviewers;
  @NotNull private final Collection<String> teamReviewers;

  public GiteeReviewersCollectionRequest(@NotNull Collection<String> reviewers,
                                          @NotNull Collection<String> teamReviewers) {
    this.reviewers = reviewers;
    this.teamReviewers = teamReviewers;
  }
}
