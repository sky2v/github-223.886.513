// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.authentication.ui

import com.intellij.collaboration.auth.ui.AccountsListModel
import com.intellij.collaboration.auth.ui.MutableAccountsListModel
import cn.osc.gitee.github.authentication.accounts.GithubAccount

class GHAccountsListModel : MutableAccountsListModel<GithubAccount, String>(),
                            AccountsListModel.WithDefault<GithubAccount, String> {
  override var defaultAccount: GithubAccount? = null
}