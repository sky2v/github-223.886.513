// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnusedDeclaration")
public class GiteeRepoWithPermissions extends GiteeRepo {
  private GiteePermissions permissions;

  @NotNull
  public GiteePermissions getPermissions() {
    return permissions;
  }
}
