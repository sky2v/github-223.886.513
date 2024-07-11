// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest

import com.intellij.openapi.project.Project
import cn.osc.gitee.api.GERepositoryCoordinates
import cn.osc.gitee.pullrequest.data.GEPRIdentifier

/**
 * [fileManagerId] is a [cn.osc.gitee.pullrequest.data.GEPRFilesManagerImpl.id] which is required to differentiate files
 * between launches of a PR toolwindow.
 * This is necessary to make the files appear in "Recent Files" correctly.
 * See [com.intellij.vcs.editor.ComplexPathVirtualFileSystem.ComplexPath.sessionId] for details.
 */
abstract class GEPRVirtualFile(fileManagerId: String,
                               project: Project,
                               repository: GERepositoryCoordinates,
                               val pullRequest: GEPRIdentifier)
  : GERepoVirtualFile(fileManagerId, project, repository) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GEPRVirtualFile) return false
    if (!super.equals(other)) return false

    if (pullRequest != other.pullRequest) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + pullRequest.hashCode()
    return result
  }
}