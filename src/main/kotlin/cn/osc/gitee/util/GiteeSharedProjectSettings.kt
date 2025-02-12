// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.util

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@State(name = "GiteeSharedProjectSettings", storages = [Storage("vcs.xml")])
class GiteeSharedProjectSettings : PersistentStateComponentWithModificationTracker<GiteeSharedProjectSettings.SettingsState> {
  private var state: SettingsState = SettingsState()

  class SettingsState : BaseState() {
    var pullRequestMergeForbidden by property(false)
  }

  var pullRequestMergeForbidden: Boolean
    get() = state.pullRequestMergeForbidden
    set(value) {
      state.pullRequestMergeForbidden = value
    }

  override fun getStateModificationCount() = state.modificationCount
  override fun getState() = state
  override fun loadState(state: SettingsState) {
    this.state = state
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project) = project.service<GiteeSharedProjectSettings>()
  }
}
