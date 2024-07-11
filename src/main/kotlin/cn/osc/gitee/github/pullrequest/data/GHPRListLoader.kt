// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.github.pullrequest.data

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import cn.osc.gitee.github.api.GHGQLRequests
import cn.osc.gitee.github.api.GHRepositoryCoordinates
import cn.osc.gitee.github.api.GHRepositoryPath
import cn.osc.gitee.github.api.GithubApiRequestExecutor
import cn.osc.gitee.github.api.data.pullrequest.GHPullRequestShort
import cn.osc.gitee.github.api.data.request.search.GithubIssueSearchType
import cn.osc.gitee.github.api.util.GithubApiSearchQueryBuilder
import cn.osc.gitee.github.api.util.SimpleGHGQLPagesLoader
import kotlin.properties.Delegates

internal class GHPRListLoader(
  progressManager: ProgressManager,
  requestExecutor: GithubApiRequestExecutor,
  repository: GHRepositoryCoordinates,
) : GHListLoaderBase<GHPullRequestShort>(progressManager) {

  var searchQuery by Delegates.observable<GHPRSearchQuery?>(null) { _, _, _ ->
    reset()
  }

  private val loader = SimpleGHGQLPagesLoader(requestExecutor, { p ->
    GHGQLRequests.PullRequest.search(repository.serverPath, buildQuery(repository.repositoryPath, searchQuery), p)
  })

  override fun canLoadMore() = !loading && (loader.hasNext || error != null)

  override fun doLoadMore(indicator: ProgressIndicator, update: Boolean) = loader.loadNext(indicator, update)

  override fun reset() {
    loader.reset()
    super.reset()
  }

  companion object {
    private fun buildQuery(repoPath: GHRepositoryPath, searchQuery: GHPRSearchQuery?): String {
      return GithubApiSearchQueryBuilder.searchQuery {
        qualifier("type", GithubIssueSearchType.pr.name)
        qualifier("repo", repoPath.toString())
        searchQuery?.buildApiSearchQuery(this)
      }
    }
  }
}
