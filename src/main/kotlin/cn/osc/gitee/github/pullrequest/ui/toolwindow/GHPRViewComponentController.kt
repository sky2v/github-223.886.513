// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.ui.toolwindow

import com.intellij.openapi.util.Key

interface GHPRViewComponentController {

  fun selectCommit(oid: String)

  fun selectChange(oid: String?, filePath: String)

  companion object {
    val KEY = Key.create<GHPRViewComponentController>("Github.PullRequests.View.Controller")
  }
}