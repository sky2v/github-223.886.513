// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest

import com.intellij.diff.impl.DiffRequestProcessor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import cn.osc.gitee.github.api.GHRepositoryCoordinates
import cn.osc.gitee.github.i18n.GithubBundle
import cn.osc.gitee.github.pullrequest.data.GHPRDataContextRepository
import cn.osc.gitee.github.pullrequest.data.GHPRIdentifier

@Suppress("EqualsOrHashCode")
internal class GHPRDiffVirtualFile(fileManagerId: String,
                                   project: Project,
                                   repository: GHRepositoryCoordinates,
                                   val pullRequest: GHPRIdentifier)
  : GHPRDiffVirtualFileBase(fileManagerId, project, repository) {

  override fun createProcessor(project: Project): DiffRequestProcessor {
    val dataDisposable = Disposer.newDisposable()
    val dataContext = GHPRDataContextRepository.getInstance(project).findContext(repository)!!
    val dataProvider = dataContext.dataProviderRepository.getDataProvider(pullRequest, dataDisposable)
    val diffRequestModel = dataProvider.diffRequestModel

    val diffProcessor = GHPRDiffRequestChainProcessor(project, diffRequestModel)
    Disposer.register(diffProcessor, dataDisposable)

    return diffProcessor
  }

  override fun getName() = "#${pullRequest.number}.diff"
  override fun getPresentableName() = GithubBundle.message("pull.request.diff.editor.title", pullRequest.number)

  override fun getPath(): String =
    (fileSystem as GHPRVirtualFileSystem).getPath(fileManagerId, project, repository, pullRequest, null, true)

  override fun getPresentablePath() = "${repository.toUrl()}/pulls/${pullRequest.number}.diff"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GHPRDiffVirtualFile) return false
    if (other.pullRequest != pullRequest) return false
    if (!super.equals(other)) return false
    return true
  }
}
