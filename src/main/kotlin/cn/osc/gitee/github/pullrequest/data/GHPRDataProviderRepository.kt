// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.data

import com.intellij.openapi.Disposable
import com.intellij.util.concurrency.annotations.RequiresEdt
import cn.osc.gitee.github.api.data.pullrequest.GHPullRequest
import cn.osc.gitee.github.pullrequest.data.provider.GHPRDataProvider

internal interface GHPRDataProviderRepository : Disposable {
  @RequiresEdt
  fun getDataProvider(id: GHPRIdentifier, disposable: Disposable): GHPRDataProvider

  @RequiresEdt
  fun findDataProvider(id: GHPRIdentifier): GHPRDataProvider?

  @RequiresEdt
  fun addDetailsLoadedListener(disposable: Disposable, listener: (GHPullRequest) -> Unit)
}