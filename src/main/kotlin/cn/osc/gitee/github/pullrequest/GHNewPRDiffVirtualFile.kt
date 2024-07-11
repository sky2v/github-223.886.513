// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.github.pullrequest

import com.intellij.diff.impl.DiffRequestProcessor
import com.intellij.openapi.project.Project
import cn.osc.gitee.github.api.GHRepositoryCoordinates
import cn.osc.gitee.github.i18n.GithubBundle
import cn.osc.gitee.github.pullrequest.data.GHPRDataContextRepository

@Suppress("EqualsOrHashCode")
internal class GHNewPRDiffVirtualFile(fileManagerId: String,
                                      project: Project,
                                      repository: GHRepositoryCoordinates)
  : GHPRDiffVirtualFileBase(fileManagerId, project, repository) {

  override fun createProcessor(project: Project): DiffRequestProcessor {
    val dataContext = GHPRDataContextRepository.getInstance(project).findContext(repository)!!
    val diffRequestModel = dataContext.newPRDiffModel

    return GHPRDiffRequestChainProcessor(project, diffRequestModel)
  }

  override fun getName() = "newPR.diff"
  override fun getPresentableName() = GithubBundle.message("pull.request.new.diff.editor.title")

  override fun getPath(): String = (fileSystem as GHPRVirtualFileSystem).getPath(fileManagerId, project, repository, null, null, true)
  override fun getPresentablePath() = "${repository.toUrl()}/pulls/newPR.diff"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GHNewPRDiffVirtualFile) return false
    if (!super.equals(other)) return false
    return true
  }
}
