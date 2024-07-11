// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.pullrequest

import com.intellij.openapi.project.Project
import cn.osc.gitee.api.GERepositoryCoordinates
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.pullrequest.data.GEPRIdentifier

@Suppress("EqualsOrHashCode")
internal class GEPRCombinedDiffPreviewVirtualFile(sourceId: String,
                                                  fileManagerId: String,
                                                  project: Project,
                                                  repository: GERepositoryCoordinates,
                                                  private val pullRequest: GEPRIdentifier) :
  GEPRCombinedDiffPreviewVirtualFileBase(sourceId, fileManagerId, project, repository) {

  override fun getName() = "#${pullRequest.number}.diff"
  override fun getPresentableName() = GiteeBundle.message("pull.request.diff.editor.title", pullRequest.number)

  override fun getPath(): String =
    (fileSystem as GEPRVirtualFileSystem).getPath(fileManagerId, project, repository, pullRequest, sourceId, true)

  override fun getPresentablePath() = "${repository.toUrl()}/pulls/${pullRequest.number}.diff"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GEPRCombinedDiffPreviewVirtualFile) return false
    if (other.pullRequest != pullRequest) return false
    if (!super.equals(other)) return false
    return true
  }
}
