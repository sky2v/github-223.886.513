// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.util

import com.intellij.openapi.progress.ProgressIndicator
import cn.osc.gitee.api.GiteeApiRequest
import cn.osc.gitee.api.GiteeApiRequestExecutor
import cn.osc.gitee.api.data.GiteeResponsePage
import java.io.IOException
import java.util.function.Predicate

object GiteeApiPagesLoader {

  @Throws(IOException::class)
  @JvmStatic
  fun <T> loadAll(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator, pagesRequest: Request<T>): List<T> {
    val result = mutableListOf<T>()
    loadAll(executor, indicator, pagesRequest) { result.addAll(it) }
    return result
  }

  @Throws(IOException::class)
  @JvmStatic
  fun <T> loadAll(executor: GiteeApiRequestExecutor,
                  indicator: ProgressIndicator,
                  pagesRequest: Request<T>,
                  pageItemsConsumer: (List<T>) -> Unit) {
    var request: GiteeApiRequest<GiteeResponsePage<T>>? = pagesRequest.initialRequest
    while (request != null) {
      val page = executor.execute(indicator, request)
      pageItemsConsumer(page.items)
      request = page.nextLink?.let(pagesRequest.urlRequestProvider)
    }
  }

  @Throws(IOException::class)
  @JvmStatic
  fun <T> find(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator, pagesRequest: Request<T>, predicate: Predicate<T>): T? {
    var request: GiteeApiRequest<GiteeResponsePage<T>>? = pagesRequest.initialRequest
    while (request != null) {
      val page = executor.execute(indicator, request)
      page.items.find { predicate.test(it) }?.let { return it }
      request = page.nextLink?.let(pagesRequest.urlRequestProvider)
    }
    return null
  }

  @Throws(IOException::class)
  @JvmStatic
  fun <T> load(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator, pagesRequest: Request<T>, maximum: Int): List<T> {
    val result = mutableListOf<T>()
    var request: GiteeApiRequest<GiteeResponsePage<T>>? = pagesRequest.initialRequest
    while (request != null) {
      val page = executor.execute(indicator, request)
      for (item in page.items) {
        result.add(item)
        if (result.size == maximum) return result
      }
      request = page.nextLink?.let(pagesRequest.urlRequestProvider)
    }
    return result
  }

  class Request<T>(val initialRequest: GiteeApiRequest<GiteeResponsePage<T>>,
                   val urlRequestProvider: (String) -> GiteeApiRequest<GiteeResponsePage<T>>)
}