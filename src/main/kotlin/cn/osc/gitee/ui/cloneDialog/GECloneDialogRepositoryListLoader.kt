// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.ui.cloneDialog

import com.intellij.ui.SingleSelectionModel
import cn.osc.gitee.authentication.accounts.GiteeAccount
import javax.swing.ListModel

interface GECloneDialogRepositoryListLoader {
  val loading: Boolean
  val listModel: ListModel<GERepositoryListItem>
  val listSelectionModel: SingleSelectionModel

  fun loadRepositories(account: GiteeAccount)
  fun clear(account: GiteeAccount)
  fun addLoadingStateListener(listener: () -> Unit)
}