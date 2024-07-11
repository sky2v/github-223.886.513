// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.pullrequest.data

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import cn.osc.gitee.api.GEGQLRequests
import cn.osc.gitee.api.GERepositoryCoordinates
import cn.osc.gitee.api.GERepositoryPath
import cn.osc.gitee.api.GiteeApiRequestExecutor
import cn.osc.gitee.api.data.pullrequest.GEPullRequestShort
import cn.osc.gitee.api.data.request.search.GiteeIssueSearchType
import cn.osc.gitee.api.util.GiteeApiSearchQueryBuilder
import cn.osc.gitee.api.util.SimpleGHGQLPagesLoader
import kotlin.properties.Delegates

internal class GEPRListLoader(
  progressManager: ProgressManager,
  requestExecutor: GiteeApiRequestExecutor,
  repository: GERepositoryCoordinates,
) : GEListLoaderBase<GEPullRequestShort>(progressManager) {

  var searchQuery by Delegates.observable<GEPRSearchQuery?>(null) { _, _, _ ->
    reset()
  }

  private val loader = SimpleGHGQLPagesLoader(requestExecutor, { p ->
    GEGQLRequests.PullRequest.search(repository.serverPath, buildQuery(repository.repositoryPath, searchQuery), p)
  })

  override fun canLoadMore() = !loading && (loader.hasNext || error != null)

  override fun doLoadMore(indicator: ProgressIndicator, update: Boolean) = loader.loadNext(indicator, update)

  override fun reset() {
    loader.reset()
    super.reset()
  }

  companion object {
    private fun buildQuery(repoPath: GERepositoryPath, searchQuery: GEPRSearchQuery?): String {
      return GiteeApiSearchQueryBuilder.searchQuery {
        qualifier("type", GiteeIssueSearchType.pr.name)
        qualifier("repo", repoPath.toString())
        searchQuery?.buildApiSearchQuery(this)
      }
    }
  }
}
