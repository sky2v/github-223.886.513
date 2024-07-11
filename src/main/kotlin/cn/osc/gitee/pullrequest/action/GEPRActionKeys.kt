// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.action

import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.vcs.FilePath
import git4idea.repo.GitRepository
import cn.osc.gitee.api.data.pullrequest.GEPullRequestShort
import cn.osc.gitee.pullrequest.data.provider.GEPRDataProvider
import cn.osc.gitee.pullrequest.ui.toolwindow.GEPRToolWindowTabComponentController

object GEPRActionKeys {
  @JvmStatic
  val GIT_REPOSITORY = DataKey.create<GitRepository>("cn.osc.gitee.pullrequest.git.repository")

  @JvmStatic
  val PULL_REQUEST_DATA_PROVIDER = DataKey.create<GEPRDataProvider>("cn.osc.gitee.pullrequest.data.provider")

  internal val PULL_REQUEST_FILES = DataKey.create<Iterable<FilePath>>("cn.osc.gitee.pullrequest.files")

  @JvmStatic
  val SELECTED_PULL_REQUEST = DataKey.create<GEPullRequestShort>("cn.osc.gitee.pullrequest.list.selected")

  @JvmStatic
  val PULL_REQUESTS_TAB_CONTROLLER = DataKey.create<GEPRToolWindowTabComponentController>(
    "cn.osc.gitee.pullrequest.tab.controller")
}