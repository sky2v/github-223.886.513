// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnusedDeclaration")
public class GiteeRepoDetailed extends GiteeRepoWithPermissions {
  private Boolean allowSquashMerge;
  private Boolean allowMergeCommit;
  private Boolean allowRebaseMerge;
  private GiteeOrg organization;
  private GiteeRepo parent;
  private GiteeRepo source;
  private Integer networkCount;
  private Integer subscribersCount;

  public boolean getAllowSquashMerge() {
    return allowSquashMerge != null ? allowSquashMerge : false;
  }

  public boolean getAllowMergeCommit() {
    return allowMergeCommit != null ? allowMergeCommit : false;
  }

  public boolean getAllowRebaseMerge() {
    return allowRebaseMerge != null ? allowRebaseMerge : false;
  }

  @Nullable
  public GiteeRepo getParent() {
    return parent;
  }

  @Nullable
  public GiteeRepo getSource() {
    return source;
  }
}
