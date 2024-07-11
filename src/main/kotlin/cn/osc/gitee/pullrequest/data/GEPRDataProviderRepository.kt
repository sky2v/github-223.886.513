// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data

import com.intellij.openapi.Disposable
import com.intellij.util.concurrency.annotations.RequiresEdt
import cn.osc.gitee.api.data.pullrequest.GEPullRequest
import cn.osc.gitee.pullrequest.data.provider.GEPRDataProvider

internal interface GEPRDataProviderRepository : Disposable {
  @RequiresEdt
  fun getDataProvider(id: GEPRIdentifier, disposable: Disposable): GEPRDataProvider

  @RequiresEdt
  fun findDataProvider(id: GEPRIdentifier): GEPRDataProvider?

  @RequiresEdt
  fun addDetailsLoadedListener(disposable: Disposable, listener: (GEPullRequest) -> Unit)
}