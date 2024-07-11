// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.util

@DslMarker
private annotation class SearchQueryDsl

@SearchQueryDsl
class GiteeApiSearchQueryBuilder {
  private val builder = StringBuilder()

  fun qualifier(name: String, value: String?) {
    if (value != null) append("$name:$value")
  }

  fun query(value: String?) {
    if (value != null) append(value)
  }

  private fun append(part: String) {
    if (builder.isNotEmpty()) builder.append(" ")
    builder.append(part)
  }

  companion object {
    @JvmStatic
    fun searchQuery(init: GiteeApiSearchQueryBuilder.() -> Unit): String {
      val query = GiteeApiSearchQueryBuilder()
      init(query)
      return query.builder.toString()
    }
  }
}