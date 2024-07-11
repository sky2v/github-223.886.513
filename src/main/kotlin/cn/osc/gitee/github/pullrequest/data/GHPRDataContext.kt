// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.data

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import cn.osc.gitee.github.api.data.pullrequest.GHPullRequest
import cn.osc.gitee.github.pullrequest.GHPRDiffRequestModel
import cn.osc.gitee.github.pullrequest.data.service.GHPRCreationService
import cn.osc.gitee.github.pullrequest.data.service.GHPRDetailsService
import cn.osc.gitee.github.pullrequest.data.service.GHPRRepositoryDataService
import cn.osc.gitee.github.pullrequest.data.service.GHPRSecurityService
import cn.osc.gitee.github.ui.avatars.GHAvatarIconsProvider

internal class GHPRDataContext(val listLoader: GHPRListLoader,
                               val listUpdatesChecker: GHPRListUpdatesChecker,
                               val dataProviderRepository: GHPRDataProviderRepository,
                               val securityService: GHPRSecurityService,
                               val repositoryDataService: GHPRRepositoryDataService,
                               val creationService: GHPRCreationService,
                               val detailsService: GHPRDetailsService,
                               val avatarIconsProvider: GHAvatarIconsProvider,
                               val filesManager: GHPRFilesManager,
                               val newPRDiffModel: GHPRDiffRequestModel) : Disposable {

  private val listenersDisposable = Disposer.newDisposable("GH PR context listeners disposable")

  init {
    listLoader.addDataListener(listenersDisposable, object : GHListLoader.ListDataListener {
      override fun onDataAdded(startIdx: Int) = listUpdatesChecker.start()
      override fun onAllDataRemoved() = listUpdatesChecker.stop()
    })
    dataProviderRepository.addDetailsLoadedListener(listenersDisposable) { details: GHPullRequest ->
      listLoader.updateData(details)
      filesManager.updateTimelineFilePresentation(details)
    }
    filesManager.addBeforeTimelineFileOpenedListener(listenersDisposable) { file ->
      val details = listLoader.loadedData.find { it.id == file.pullRequest.id }
                    ?: dataProviderRepository.findDataProvider(file.pullRequest)?.detailsData?.loadedDetails
      if (details != null) filesManager.updateTimelineFilePresentation(details)
    }
  }

  override fun dispose() {
    Disposer.dispose(filesManager)
    Disposer.dispose(listenersDisposable)
    Disposer.dispose(dataProviderRepository)
    Disposer.dispose(listLoader)
    Disposer.dispose(listUpdatesChecker)
    Disposer.dispose(repositoryDataService)
  }
}