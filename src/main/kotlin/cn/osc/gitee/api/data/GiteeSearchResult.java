// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("UnusedDeclaration")
public class GiteeSearchResult<T> {
  private List<T> items;
  private Integer totalCount;
  private Boolean incompleteResults;

  @NotNull
  public List<T> getItems() {
    return items;
  }
}
