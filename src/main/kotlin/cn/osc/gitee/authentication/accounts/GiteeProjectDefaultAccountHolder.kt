// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.authentication.accounts

import com.intellij.collaboration.auth.PersistentDefaultAccountHolder
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsNotifier
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.util.GiteeNotificationIdsHolder
import cn.osc.gitee.util.GiteeNotifications
import cn.osc.gitee.util.GiteeUtil

/**
 * Handles default Gitee account for project
 */
@State(name = "GiteeDefaultAccount", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)], reportStatistic = false)
internal class GiteeProjectDefaultAccountHolder(project: Project)
  : PersistentDefaultAccountHolder<GiteeAccount>(project) {

  override fun accountManager() = service<GEAccountManager>()

  override fun notifyDefaultAccountMissing() = runInEdt {
    val title = GiteeBundle.message("accounts.default.missing")
    GiteeUtil.LOG.info("${title}; ${""}")
    VcsNotifier.IMPORTANT_ERROR_NOTIFICATION.createNotification(title, NotificationType.WARNING)
      .setDisplayId(GiteeNotificationIdsHolder.MISSING_DEFAULT_ACCOUNT)
      .addAction(GiteeNotifications.getConfigureAction(project))
      .notify(project)
  }
}