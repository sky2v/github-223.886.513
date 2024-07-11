// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.api

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import cn.osc.gitee.github.api.GithubApiRequestExecutor.*
import cn.osc.gitee.github.authentication.accounts.GithubAccount
import cn.osc.gitee.github.util.GHCompatibilityUtil

/**
 * Allows to acquire API executor without exposing the auth token to external code
 */
@Deprecated("Use cn.osc.gitee.github.api.GithubApiRequestExecutor.Factory.Companion directly")
class GithubApiRequestExecutorManager {

  companion object {
    @JvmStatic
    fun getInstance(): GithubApiRequestExecutorManager = service()
  }

  @Deprecated("One-time use executor should not be persisted")
  @RequiresBackgroundThread
  fun getExecutor(account: GithubAccount, project: Project): GithubApiRequestExecutor? {
    val token = GHCompatibilityUtil.getOrRequestToken(account, project) ?: return null
    return Factory.getInstance().create(token)
  }
}