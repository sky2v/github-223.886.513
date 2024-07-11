// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.authentication.accounts

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.intellij.openapi.components.service
import com.intellij.openapi.util.text.StringUtil
import cn.osc.gitee.api.GiteeServerPath

internal class GiteeAccountsStatisticsCollector : ApplicationUsagesCollector() {

  override fun getMetrics(): Set<MetricEvent> {
    val accountManager = service<GEAccountManager>()
    val hasAccountsWithNonDefaultHost = accountManager.accountsState.value.any {
      !StringUtil.equalsIgnoreCase(it.server.host, GiteeServerPath.DEFAULT_HOST)
    }

    return setOf(ACCOUNTS.metric(accountManager.accountsState.value.size, hasAccountsWithNonDefaultHost))
  }

  override fun getGroup(): EventLogGroup {
    return GROUP
  }

  companion object {
    private val GROUP = EventLogGroup("vcs.github", 3)
    private val ACCOUNTS = GROUP.registerEvent("accounts", EventFields.Count, EventFields.Boolean("has_enterprise"))
  }
}