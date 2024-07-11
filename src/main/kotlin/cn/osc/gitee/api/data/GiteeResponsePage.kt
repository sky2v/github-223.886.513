// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data

import com.intellij.collaboration.api.util.LinkHttpHeaderValue

class GiteeResponsePage<T>(val items: List<T>,
                            val firstLink: String? = null,
                            val prevLink: String? = null,
                            val nextLink: String? = null,
                            val lastLink: String? = null) {

  val hasNext = nextLink != null

  constructor(items: List<T>, linkHeaderValue: LinkHttpHeaderValue?)
    : this(items, linkHeaderValue?.firstLink, linkHeaderValue?.prevLink, linkHeaderValue?.nextLink, linkHeaderValue?.lastLink)
}


