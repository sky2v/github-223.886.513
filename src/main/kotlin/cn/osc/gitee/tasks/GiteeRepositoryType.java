// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.tasks;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.tasks.TaskRepository;
import com.intellij.tasks.TaskState;
import com.intellij.tasks.config.TaskRepositoryEditor;
import com.intellij.tasks.impl.BaseRepositoryType;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.EnumSet;

final class GiteeRepositoryType extends BaseRepositoryType<GiteeRepository> {
  @NotNull
  @Override
  public String getName() {
    return "GitHub";
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AllIcons.Vcs.Vendors.Github;
  }

  @NotNull
  @Override
  public TaskRepository createRepository() {
    return new GiteeRepository(this);
  }

  @Override
  public Class<GiteeRepository> getRepositoryClass() {
    return GiteeRepository.class;
  }

  @NotNull
  @Override
  public TaskRepositoryEditor createEditor(GiteeRepository repository,
                                           Project project,
                                           Consumer<? super GiteeRepository> changeListener) {
    return new GiteeRepositoryEditor(project, repository, changeListener);
  }

  @Override
  public EnumSet<TaskState> getPossibleTaskStates() {
    return EnumSet.of(TaskState.OPEN, TaskState.RESOLVED);
  }

}
