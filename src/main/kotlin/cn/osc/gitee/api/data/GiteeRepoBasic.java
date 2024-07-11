// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import cn.osc.gitee.api.GERepositoryPath;

import java.util.Objects;

@SuppressWarnings("UnusedDeclaration")
public class GiteeRepoBasic {
  private Long id;
  //private String nodeId;
  private String name;
  private String fullName;
  private GiteeUser owner;
  @JsonProperty("private")
  private Boolean isPrivate;
  private String htmlUrl;
  private String description;
  @JsonProperty("fork")
  private Boolean isFork;

  private String url;
  //urls

  @NotNull
  public String getName() {
    return name;
  }

  @NotNull
  public String getDescription() {
    return StringUtil.notNullize(description);
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public boolean isFork() {
    return isFork;
  }

  @NotNull
  public String getUrl() {
    return url;
  }

  @NotNull
  public String getHtmlUrl() {
    return htmlUrl;
  }

  @NotNull
  public GiteeUser getOwner() {
    return owner;
  }


  @NotNull
  public String getUserName() {
    return getOwner().getLogin();
  }

  @NotNull
  public String getFullName() {
    return getUserName() + "/" + getName();
  }

  @NotNull
  public GERepositoryPath getFullPath() {
    return new GERepositoryPath(getUserName(), getName());
  }

  @Override
  public String toString() {
    return "GiteeRepo{" +
           "id=" + id +
           ", name='" + name + '\'' +
           '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GiteeRepoBasic)) return false;
    GiteeRepoBasic basic = (GiteeRepoBasic)o;
    return id.equals(basic.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
