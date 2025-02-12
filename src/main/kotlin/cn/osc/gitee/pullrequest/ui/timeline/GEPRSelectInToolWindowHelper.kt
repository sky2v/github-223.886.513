// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.ui.timeline

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import cn.osc.gitee.pullrequest.GEPRToolWindowController
import cn.osc.gitee.pullrequest.data.GEPRIdentifier

class GEPRSelectInToolWindowHelper(private val project: Project, private val pullRequest: GEPRIdentifier) {

  fun selectCommit(oid: String) {
    project.service<GEPRToolWindowController>().activate { twctr ->
      twctr.componentController?.viewPullRequest(pullRequest) {
        it?.selectCommit(oid)
      }
    }
  }

  fun selectChange(oid: String?, filePath: String) {
    project.service<GEPRToolWindowController>().activate { twctr ->
      twctr.componentController?.viewPullRequest(pullRequest) {
        it?.selectChange(oid, filePath)
        twctr.componentController?.openPullRequestDiff(pullRequest, false)
      }
    }
  }

}