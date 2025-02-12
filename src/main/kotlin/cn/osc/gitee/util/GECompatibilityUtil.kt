// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.util

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import cn.osc.gitee.api.GiteeServerPath
import cn.osc.gitee.authentication.GEAccountsUtil
import cn.osc.gitee.authentication.accounts.GEAccountManager
import cn.osc.gitee.authentication.accounts.GiteeAccount

/**
 * Temporary util methods for maintaining compatibility
 */
object GECompatibilityUtil {
  @JvmStatic
  fun requestNewAccountForServer(serverPath: GiteeServerPath, project: Project): GiteeAccount? =
    GEAccountsUtil.requestNewAccount(serverPath, login = null, project = project)?.account

  @RequiresBackgroundThread
  @JvmStatic
  fun getOrRequestToken(account: GiteeAccount, project: Project): String? {
    val accountManager = service<GEAccountManager>()
    val modality = ProgressManager.getInstance().currentProgressModality ?: ModalityState.any()
    return runBlocking {
      accountManager.findCredentials(account)
      ?: withContext(Dispatchers.EDT + modality.asContextElement()) {
        GEAccountsUtil.requestNewToken(account, project)
      }
    }
  }
}