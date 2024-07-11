// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.github.pullrequest

import com.intellij.openapi.project.Project
import cn.osc.gitee.github.api.GHRepositoryCoordinates
import cn.osc.gitee.github.i18n.GithubBundle
import cn.osc.gitee.github.pullrequest.data.GHPRIdentifier

@Suppress("EqualsOrHashCode")
internal class GHPRCombinedDiffPreviewVirtualFile(sourceId: String,
                                                  fileManagerId: String,
                                                  project: Project,
                                                  repository: GHRepositoryCoordinates,
                                                  private val pullRequest: GHPRIdentifier) :
  GHPRCombinedDiffPreviewVirtualFileBase(sourceId, fileManagerId, project, repository) {

  override fun getName() = "#${pullRequest.number}.diff"
  override fun getPresentableName() = GithubBundle.message("pull.request.diff.editor.title", pullRequest.number)

  override fun getPath(): String =
    (fileSystem as GHPRVirtualFileSystem).getPath(fileManagerId, project, repository, pullRequest, sourceId, true)

  override fun getPresentablePath() = "${repository.toUrl()}/pulls/${pullRequest.number}.diff"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GHPRCombinedDiffPreviewVirtualFile) return false
    if (other.pullRequest != pullRequest) return false
    if (!super.equals(other)) return false
    return true
  }
}
