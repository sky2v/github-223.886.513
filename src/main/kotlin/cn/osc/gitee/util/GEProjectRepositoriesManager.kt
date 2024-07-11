// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@Deprecated("Use GEHostedRepositoriesManager")
@Service
class GEProjectRepositoriesManager(private val project: Project) : Disposable {

  private val scope = CoroutineScope(SupervisorJob())
  private val repositoriesManager: GEHostedRepositoriesManager get() = project.service()

  val knownRepositories: Set<GEGitRepositoryMapping>
    get() = repositoriesManager.knownRepositoriesState.value

  init {
    scope.launch {
      repositoriesManager.knownRepositoriesState.collect {
        ApplicationManager.getApplication().messageBus.syncPublisher(LIST_CHANGES_TOPIC).repositoryListChanged(it, project)
      }
    }
  }

  interface ListChangeListener {
    fun repositoryListChanged(newList: Set<GEGitRepositoryMapping>, project: Project)
  }

  override fun dispose() {
    scope.cancel()
  }

  @Deprecated("Use GEHostedRepositoriesManager", level = DeprecationLevel.ERROR)
  companion object {
    @Deprecated("Use GEHostedRepositoriesManager.knownRepositoriesState")
    @JvmField
    @Topic.AppLevel
    val LIST_CHANGES_TOPIC = Topic(ListChangeListener::class.java, Topic.BroadcastDirection.NONE)
  }
}