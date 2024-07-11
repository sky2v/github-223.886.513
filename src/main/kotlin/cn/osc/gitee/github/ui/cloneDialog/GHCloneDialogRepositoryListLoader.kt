// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.github.ui.cloneDialog

import com.intellij.ui.SingleSelectionModel
import cn.osc.gitee.github.authentication.accounts.GithubAccount
import javax.swing.ListModel

interface GHCloneDialogRepositoryListLoader {
  val loading: Boolean
  val listModel: ListModel<GHRepositoryListItem>
  val listSelectionModel: SingleSelectionModel

  fun loadRepositories(account: GithubAccount)
  fun clear(account: GithubAccount)
  fun addLoadingStateListener(listener: () -> Unit)
}