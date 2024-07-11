// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data

import com.intellij.diff.editor.DiffVirtualFileBase
import com.intellij.openapi.Disposable
import cn.osc.gitee.api.data.pullrequest.GEPullRequestShort
import cn.osc.gitee.pullrequest.GEPRTimelineVirtualFile

internal interface GEPRFilesManager : Disposable {
  fun createOrGetNewPRDiffFile(sourceId: String, combinedDiff: Boolean): DiffVirtualFileBase
  fun createOrGetDiffFile(pullRequest: GEPRIdentifier, sourceId: String, combinedDiff: Boolean): DiffVirtualFileBase

  fun createAndOpenTimelineFile(pullRequest: GEPRIdentifier, requestFocus: Boolean)

  fun createAndOpenDiffFile(pullRequest: GEPRIdentifier, requestFocus: Boolean)

  fun createAndOpenDiffPreviewFile(pullRequest: GEPRIdentifier, sourceId: String, requestFocus: Boolean): DiffVirtualFileBase

  fun createAndOpenNewPRDiffPreviewFile(sourceId: String, combinedDiff: Boolean, requestFocus: Boolean): DiffVirtualFileBase

  fun findTimelineFile(pullRequest: GEPRIdentifier): GEPRTimelineVirtualFile?

  fun findDiffFile(pullRequest: GEPRIdentifier): DiffVirtualFileBase?

  fun updateTimelineFilePresentation(details: GEPullRequestShort)

  fun addBeforeTimelineFileOpenedListener(disposable: Disposable, listener: (file: GEPRTimelineVirtualFile) -> Unit)
}
