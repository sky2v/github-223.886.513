// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest

import com.intellij.collaboration.async.disposingScope
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.eventLog.events.PrimitiveEventField
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import cn.osc.gitee.api.GiteeServerPath
import cn.osc.gitee.api.data.GEEnterpriseServerMeta
import cn.osc.gitee.api.data.GiteePullRequestMergeMethod
import cn.osc.gitee.authentication.accounts.GEAccountManager
import cn.osc.gitee.util.GEEnterpriseServerMetadataLoader

object GEPRStatisticsCollector {

  private val COUNTERS_GROUP = EventLogGroup("vcs.github.pullrequest.counters", 2)

  class Counters : CounterUsagesCollector() {
    override fun getGroup() = COUNTERS_GROUP
  }

  private val TIMELINE_OPENED_EVENT = COUNTERS_GROUP.registerEvent("timeline.opened", EventFields.Int("count"))
  private val DIFF_OPENED_EVENT = COUNTERS_GROUP.registerEvent("diff.opened", EventFields.Int("count"))
  private val MERGED_EVENT = COUNTERS_GROUP.registerEvent("merged", EventFields.Enum<GiteePullRequestMergeMethod>("method") {
    it.name.toUpperCase()
  })
  private val anonymizedId = object : PrimitiveEventField<String>() {

    override val name = "anonymized_id"

    override fun addData(fuData: FeatureUsageData, value: String) {
      fuData.addAnonymizedId(value)
    }

    override val validationRule: List<String>
      get() = listOf("{regexp#hash}")
  }
  private val SERVER_META_EVENT = COUNTERS_GROUP.registerEvent("server.meta.collected", anonymizedId, EventFields.Version)

  fun logTimelineOpened(project: Project) {
    val count = FileEditorManager.getInstance(project).openFiles.count { it is GEPRTimelineVirtualFile }
    TIMELINE_OPENED_EVENT.log(project, count)
  }

  fun logDiffOpened(project: Project) {
    val count = FileEditorManager.getInstance(project).openFiles.count { it is GEPRDiffVirtualFileBase
                                                                         || it is GEPRCombinedDiffPreviewVirtualFileBase }
    DIFF_OPENED_EVENT.log(project, count)
  }

  fun logMergedEvent(method: GiteePullRequestMergeMethod) {
    MERGED_EVENT.log(method)
  }

  fun logEnterpriseServerMeta(project: Project, server: GiteeServerPath, meta: GEEnterpriseServerMeta) {
    SERVER_META_EVENT.log(project, server.toUrl(), meta.installedVersion)
  }
}

@Service
private class GEServerVersionsCollector(private val project: Project) : Disposable {

  private val scope = disposingScope()

  init {
    val accountsFlow = project.service<GEAccountManager>().accountsState
    scope.launch {
      accountsFlow.collect {
        for (account in it) {
          val server = account.server
          if (server.isGithubDotCom) continue

          //TODO: load with auth to avoid rate-limit
          try {
            val metadata = service<GEEnterpriseServerMetadataLoader>().loadMetadata(server).await()
            GEPRStatisticsCollector.logEnterpriseServerMeta(project, server, metadata)
          }
          catch (ignore: Exception) {
          }
        }
      }
    }
  }

  class Initializer : StartupActivity.Background {
    override fun runActivity(project: Project) {
      //init service to start version checks
      project.service<GEServerVersionsCollector>()
    }
  }

  override fun dispose() = Unit
}
