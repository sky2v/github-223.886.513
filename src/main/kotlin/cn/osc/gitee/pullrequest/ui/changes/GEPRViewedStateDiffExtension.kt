// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.ui.changes

import com.intellij.diff.DiffContext
import com.intellij.diff.DiffExtension
import com.intellij.diff.FrameDiffTool
import com.intellij.diff.requests.DiffRequest

internal class GEPRViewedStateDiffExtension : DiffExtension() {
  override fun onViewerCreated(viewer: FrameDiffTool.DiffViewer, context: DiffContext, request: DiffRequest) {
    val viewedStateSupport = request.getUserData(GEPRViewedStateDiffSupport.KEY) ?: return
    val file = request.getUserData(GEPRViewedStateDiffSupport.PULL_REQUEST_FILE) ?: return

    viewedStateSupport.markViewed(file)
  }
}