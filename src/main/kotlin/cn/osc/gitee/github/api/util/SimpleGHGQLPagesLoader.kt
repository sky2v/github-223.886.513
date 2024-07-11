// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.api.util

import com.intellij.collaboration.api.dto.GraphQLPagedResponseDataDTO
import com.intellij.openapi.progress.ProgressIndicator
import cn.osc.gitee.github.api.GithubApiRequest
import cn.osc.gitee.github.api.GithubApiRequestExecutor
import cn.osc.gitee.github.api.data.graphql.GHGQLRequestPagination
import cn.osc.gitee.github.api.data.request.GithubRequestPagination

class SimpleGHGQLPagesLoader<T>(executor: GithubApiRequestExecutor,
                                requestProducer: (GHGQLRequestPagination) -> GithubApiRequest.Post<GraphQLPagedResponseDataDTO<T>>,
                                supportsTimestampUpdates: Boolean = false,
                                pageSize: Int = GithubRequestPagination.DEFAULT_PAGE_SIZE)
  : GHGQLPagesLoader<GraphQLPagedResponseDataDTO<T>, List<T>>(executor, requestProducer, supportsTimestampUpdates, pageSize) {

  fun loadAll(progressIndicator: ProgressIndicator): List<T> {
    val list = mutableListOf<T>()
    while (hasNext) {
      loadNext(progressIndicator)?.let { list.addAll(it) }
    }
    return list
  }

  override fun extractPageInfo(result: GraphQLPagedResponseDataDTO<T>) = result.pageInfo

  override fun extractResult(result: GraphQLPagedResponseDataDTO<T>) = result.nodes
}