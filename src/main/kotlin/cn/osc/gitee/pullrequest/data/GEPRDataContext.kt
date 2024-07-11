// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import cn.osc.gitee.api.data.pullrequest.GEPullRequest
import cn.osc.gitee.pullrequest.GEPRDiffRequestModel
import cn.osc.gitee.pullrequest.data.service.GEPRCreationService
import cn.osc.gitee.pullrequest.data.service.GEPRDetailsService
import cn.osc.gitee.pullrequest.data.service.GEPRRepositoryDataService
import cn.osc.gitee.pullrequest.data.service.GEPRSecurityService
import cn.osc.gitee.ui.avatars.GEAvatarIconsProvider

internal class GEPRDataContext(val listLoader: GEPRListLoader,
                               val listUpdatesChecker: GEPRListUpdatesChecker,
                               val dataProviderRepository: GEPRDataProviderRepository,
                               val securityService: GEPRSecurityService,
                               val repositoryDataService: GEPRRepositoryDataService,
                               val creationService: GEPRCreationService,
                               val detailsService: GEPRDetailsService,
                               val avatarIconsProvider: GEAvatarIconsProvider,
                               val filesManager: GEPRFilesManager,
                               val newPRDiffModel: GEPRDiffRequestModel) : Disposable {

  private val listenersDisposable = Disposer.newDisposable("GE PR context listeners disposable")

  init {
    listLoader.addDataListener(listenersDisposable, object : GEListLoader.ListDataListener {
      override fun onDataAdded(startIdx: Int) = listUpdatesChecker.start()
      override fun onAllDataRemoved() = listUpdatesChecker.stop()
    })
    dataProviderRepository.addDetailsLoadedListener(listenersDisposable) { details: GEPullRequest ->
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