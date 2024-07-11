// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.issue

import com.intellij.openapi.progress.ProgressIndicator
import cn.osc.gitee.api.GERepositoryPath
import cn.osc.gitee.api.GiteeApiRequestExecutor
import cn.osc.gitee.api.GiteeApiRequests
import cn.osc.gitee.api.GiteeServerPath
import cn.osc.gitee.api.data.GiteeIssue
import cn.osc.gitee.api.data.GiteeSearchedIssue
import cn.osc.gitee.api.util.GiteeApiPagesLoader
import java.io.IOException

object GiteeIssuesLoadingHelper {
  @JvmOverloads
  @JvmStatic
  @Throws(IOException::class)
  fun load(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator, server: GiteeServerPath,
           owner: String, repo: String, withClosed: Boolean, maximum: Int = 100, assignee: String? = null): List<GiteeIssue> {
    return GiteeApiPagesLoader.load(executor, indicator,
                                     GiteeApiRequests.Repos.Issues.pages(server, owner, repo,
                                                                           if (withClosed) "all" else "open", assignee), maximum)
  }

  @JvmOverloads
  @JvmStatic
  @Throws(IOException::class)
  fun search(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator, server: GiteeServerPath,
             owner: String, repo: String, withClosed: Boolean, assignee: String? = null, query: String? = null)
    : List<GiteeSearchedIssue> {

    return GiteeApiPagesLoader.loadAll(executor, indicator,
                                        GiteeApiRequests.Search.Issues.pages(server,
                                                                              GERepositoryPath(owner,
                                                                                               repo),
                                                                              if (withClosed) null else "open", assignee, query))
  }
}