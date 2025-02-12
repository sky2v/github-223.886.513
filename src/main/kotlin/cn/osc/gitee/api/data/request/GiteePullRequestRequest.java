// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data.request;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class GiteePullRequestRequest {
  @NotNull private final String title;
  @NotNull private final String body;
  @NotNull private final String head; // branch with changes
  @NotNull private final String base; // branch requested to

  public GiteePullRequestRequest(@NotNull String title, @NotNull String description, @NotNull String head, @NotNull String base) {
    this.title = title;
    this.body = description;
    this.head = head;
    this.base = base;
  }
}
