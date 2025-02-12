// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.authentication.accounts

import com.intellij.collaboration.auth.AccountsRepository
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "GiteeAccounts", storages = [
  Storage(value = "github.xml"),
  Storage(value = "github_settings.xml", deprecated = true)
], reportStatistic = false, category = SettingsCategory.TOOLS)
internal class GEPersistentAccounts
  : AccountsRepository<GiteeAccount>,
    PersistentStateComponent<Array<GiteeAccount>> {

  private var state = emptyArray<GiteeAccount>()

  override var accounts: Set<GiteeAccount>
    get() = state.toSet()
    set(value) {
      state = value.toTypedArray()
    }

  override fun getState(): Array<GiteeAccount> = state

  override fun loadState(state: Array<GiteeAccount>) {
    this.state = state
  }
}