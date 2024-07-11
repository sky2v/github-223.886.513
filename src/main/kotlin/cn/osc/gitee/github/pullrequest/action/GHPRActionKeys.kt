// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.action

import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.vcs.FilePath
import git4idea.repo.GitRepository
import cn.osc.gitee.github.api.data.pullrequest.GHPullRequestShort
import cn.osc.gitee.github.pullrequest.data.provider.GHPRDataProvider
import cn.osc.gitee.github.pullrequest.ui.toolwindow.GHPRToolWindowTabComponentController

object GHPRActionKeys {
  @JvmStatic
  val GIT_REPOSITORY = DataKey.create<GitRepository>("cn.osc.gitee.github.pullrequest.git.repository")

  @JvmStatic
  val PULL_REQUEST_DATA_PROVIDER = DataKey.create<GHPRDataProvider>("cn.osc.gitee.github.pullrequest.data.provider")

  internal val PULL_REQUEST_FILES = DataKey.create<Iterable<FilePath>>("cn.osc.gitee.github.pullrequest.files")

  @JvmStatic
  val SELECTED_PULL_REQUEST = DataKey.create<GHPullRequestShort>("cn.osc.gitee.github.pullrequest.list.selected")

  @JvmStatic
  val PULL_REQUESTS_TAB_CONTROLLER = DataKey.create<GHPRToolWindowTabComponentController>(
    "cn.osc.gitee.github.pullrequest.tab.controller")
}