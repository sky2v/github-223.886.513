// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data.request;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class GiteeAuthorizationCreateRequest {
  @NotNull private final List<String> scopes;

  @Nullable private final String note;
  @Nullable private final String noteUrl;

  public GiteeAuthorizationCreateRequest(@NotNull List<String> scopes, @Nullable String note, @Nullable String noteUrl) {
    this.scopes = scopes;
    this.note = note;
    this.noteUrl = noteUrl;
  }
}
