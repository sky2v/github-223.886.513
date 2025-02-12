// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.ui.cloneDialog

import cn.osc.gitee.api.data.GiteeRepo
import cn.osc.gitee.api.data.GiteeUser
import cn.osc.gitee.authentication.accounts.GiteeAccount

sealed class GERepositoryListItem(
  val account: GiteeAccount
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as GERepositoryListItem

    if (account != other.account) return false

    return true
  }

  override fun hashCode(): Int {
    return account.hashCode()
  }

  class Repo(
    account: GiteeAccount,
    val user: GiteeUser,
    val repo: GiteeRepo
  ) : GERepositoryListItem(account) {

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      if (!super.equals(other)) return false

      other as Repo

      if (user != other.user) return false
      if (repo != other.repo) return false

      return true
    }

    override fun hashCode(): Int {
      var result = super.hashCode()
      result = 31 * result + user.hashCode()
      result = 31 * result + repo.hashCode()
      return result
    }
  }

  class Error(account: GiteeAccount, val error: Throwable) : GERepositoryListItem(account) {

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      if (!super.equals(other)) return false

      other as Error

      if (error != other.error) return false

      return true
    }

    override fun hashCode(): Int {
      var result = super.hashCode()
      result = 31 * result + error.hashCode()
      return result
    }
  }
}