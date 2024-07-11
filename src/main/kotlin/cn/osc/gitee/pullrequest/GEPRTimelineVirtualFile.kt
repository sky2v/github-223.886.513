// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest

import com.intellij.ide.actions.SplitAction
import com.intellij.openapi.project.Project
import cn.osc.gitee.api.GERepositoryCoordinates
import cn.osc.gitee.api.data.pullrequest.GEPullRequestShort
import cn.osc.gitee.pullrequest.data.GEPRIdentifier
import cn.osc.gitee.ui.util.GEUIUtil
import javax.swing.Icon
import kotlin.properties.Delegates.observable

@Suppress("EqualsOrHashCode")
internal class GEPRTimelineVirtualFile(fileManagerId: String,
                                       project: Project,
                                       repository: GERepositoryCoordinates,
                                       pullRequest: GEPRIdentifier)
  : GEPRVirtualFile(fileManagerId, project, repository, pullRequest) {

  var details: GEPullRequestShort? by observable(pullRequest as? GEPullRequestShort) { _, _, _ ->
    modificationStamp = modificationStamp++
  }

  init {
    putUserData(SplitAction.FORBID_TAB_SPLIT, true)
    isWritable = false
  }

  override fun getName() = "#${pullRequest.number}"
  override fun getPresentableName() = details?.let { "${it.title} $name" } ?: name

  override fun getPath(): String = (fileSystem as GEPRVirtualFileSystem).getPath(fileManagerId, project, repository, pullRequest, null)
  override fun getPresentablePath() = details?.url ?: "${repository.toUrl()}/pulls/${pullRequest.number}"

  fun getIcon(): Icon? = details?.let { GEUIUtil.getPullRequestStateIcon(it.state, it.isDraft) }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GEPRTimelineVirtualFile) return false
    if (!super.equals(other)) return false
    return true
  }
}
