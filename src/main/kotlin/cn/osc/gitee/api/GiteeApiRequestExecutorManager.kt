// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import cn.osc.gitee.api.GiteeApiRequestExecutor.*
import cn.osc.gitee.authentication.accounts.GiteeAccount
import cn.osc.gitee.util.GECompatibilityUtil

/**
 * Allows to acquire API executor without exposing the auth token to external code
 */
@Deprecated("Use cn.osc.gitee.api.GiteeApiRequestExecutor.Factory.Companion directly")
class GiteeApiRequestExecutorManager {

  companion object {
    @JvmStatic
    fun getInstance(): GiteeApiRequestExecutorManager = service()
  }

  @Deprecated("One-time use executor should not be persisted")
  @RequiresBackgroundThread
  fun getExecutor(account: GiteeAccount, project: Project): GiteeApiRequestExecutor? {
    val token = GECompatibilityUtil.getOrRequestToken(account, project) ?: return null
    return Factory.getInstance().create(token)
  }
}