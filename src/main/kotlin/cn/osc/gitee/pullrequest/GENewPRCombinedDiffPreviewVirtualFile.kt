// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.pullrequest

import com.intellij.openapi.project.Project
import cn.osc.gitee.api.GERepositoryCoordinates
import cn.osc.gitee.i18n.GiteeBundle

@Suppress("EqualsOrHashCode")
internal class GENewPRCombinedDiffPreviewVirtualFile(sourceId: String,
                                                     fileManagerId: String,
                                                     project: Project,
                                                     repository: GERepositoryCoordinates) :
  GEPRCombinedDiffPreviewVirtualFileBase(sourceId, fileManagerId, project, repository) {

  override fun getName() = "newPR.diff"
  override fun getPresentableName() = GiteeBundle.message("pull.request.new.diff.editor.title")

  override fun getPath(): String = (fileSystem as GEPRVirtualFileSystem).getPath(fileManagerId, project, repository, null, sourceId, true)
  override fun getPresentablePath() = "${repository.toUrl()}/pulls/newPR.diff"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GENewPRCombinedDiffPreviewVirtualFile) return false
    if (!super.equals(other)) return false
    return true
  }
}
