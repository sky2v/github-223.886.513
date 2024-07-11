// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.authentication.ui

import com.intellij.collaboration.auth.ui.AccountsListModel
import com.intellij.collaboration.auth.ui.MutableAccountsListModel
import cn.osc.gitee.authentication.accounts.GiteeAccount

class GEAccountsListModel : MutableAccountsListModel<GiteeAccount, String>(),
                            AccountsListModel.WithDefault<GiteeAccount, String> {
  override var defaultAccount: GiteeAccount? = null
}