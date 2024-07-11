// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.data

import com.intellij.diff.editor.DiffVirtualFileBase
import com.intellij.openapi.Disposable
import cn.osc.gitee.github.api.data.pullrequest.GHPullRequestShort
import cn.osc.gitee.github.pullrequest.GHPRTimelineVirtualFile

internal interface GHPRFilesManager : Disposable {
  fun createOrGetNewPRDiffFile(sourceId: String, combinedDiff: Boolean): DiffVirtualFileBase
  fun createOrGetDiffFile(pullRequest: GHPRIdentifier, sourceId: String, combinedDiff: Boolean): DiffVirtualFileBase

  fun createAndOpenTimelineFile(pullRequest: GHPRIdentifier, requestFocus: Boolean)

  fun createAndOpenDiffFile(pullRequest: GHPRIdentifier, requestFocus: Boolean)

  fun createAndOpenDiffPreviewFile(pullRequest: GHPRIdentifier, sourceId: String, requestFocus: Boolean): DiffVirtualFileBase

  fun createAndOpenNewPRDiffPreviewFile(sourceId: String, combinedDiff: Boolean, requestFocus: Boolean): DiffVirtualFileBase

  fun findTimelineFile(pullRequest: GHPRIdentifier): GHPRTimelineVirtualFile?

  fun findDiffFile(pullRequest: GHPRIdentifier): DiffVirtualFileBase?

  fun updateTimelineFilePresentation(details: GHPullRequestShort)

  fun addBeforeTimelineFileOpenedListener(disposable: Disposable, listener: (file: GHPRTimelineVirtualFile) -> Unit)
}
