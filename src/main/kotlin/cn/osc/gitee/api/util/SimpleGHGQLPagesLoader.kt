// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.util

import com.intellij.collaboration.api.dto.GraphQLPagedResponseDataDTO
import com.intellij.openapi.progress.ProgressIndicator
import cn.osc.gitee.api.GiteeApiRequest
import cn.osc.gitee.api.GiteeApiRequestExecutor
import cn.osc.gitee.api.data.graphql.GEGQLRequestPagination
import cn.osc.gitee.api.data.request.GiteeRequestPagination

class SimpleGHGQLPagesLoader<T>(executor: GiteeApiRequestExecutor,
                                requestProducer: (GEGQLRequestPagination) -> GiteeApiRequest.Post<GraphQLPagedResponseDataDTO<T>>,
                                supportsTimestampUpdates: Boolean = false,
                                pageSize: Int = GiteeRequestPagination.DEFAULT_PAGE_SIZE)
  : GEGQLPagesLoader<GraphQLPagedResponseDataDTO<T>, List<T>>(executor, requestProducer, supportsTimestampUpdates, pageSize) {

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