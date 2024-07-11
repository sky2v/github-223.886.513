// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.github.authentication.accounts

import com.intellij.collaboration.auth.PersistentDefaultAccountHolder
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsNotifier
import cn.osc.gitee.github.i18n.GithubBundle
import cn.osc.gitee.github.util.GithubNotificationIdsHolder
import cn.osc.gitee.github.util.GithubNotifications
import cn.osc.gitee.github.util.GithubUtil

/**
 * Handles default Github account for project
 */
@State(name = "GithubDefaultAccount", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)], reportStatistic = false)
internal class GithubProjectDefaultAccountHolder(project: Project)
  : PersistentDefaultAccountHolder<GithubAccount>(project) {

  override fun accountManager() = service<GHAccountManager>()

  override fun notifyDefaultAccountMissing() = runInEdt {
    val title = GithubBundle.message("accounts.default.missing")
    GithubUtil.LOG.info("${title}; ${""}")
    VcsNotifier.IMPORTANT_ERROR_NOTIFICATION.createNotification(title, NotificationType.WARNING)
      .setDisplayId(GithubNotificationIdsHolder.MISSING_DEFAULT_ACCOUNT)
      .addAction(GithubNotifications.getConfigureAction(project))
      .notify(project)
  }
}