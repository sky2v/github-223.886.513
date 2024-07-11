// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.pullrequest.ui.toolwindow

import git4idea.remote.hosting.knownRepositories
import com.intellij.collaboration.ui.CollaborationToolsUIUtil
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ClearableLazyValue
import com.intellij.openapi.util.Disposer
import com.intellij.ui.CollectionListModel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.IJSwingUtilities
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.Nls
import cn.osc.gitee.api.data.pullrequest.GEPullRequestShort
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.pullrequest.action.GEPRActionKeys
import cn.osc.gitee.pullrequest.config.GiteePullRequestsProjectUISettings
import cn.osc.gitee.pullrequest.data.GEListLoader
import cn.osc.gitee.pullrequest.data.GEPRDataContext
import cn.osc.gitee.pullrequest.data.GEPRIdentifier
import cn.osc.gitee.pullrequest.ui.toolwindow.create.GEPRCreateComponentHolder
import cn.osc.gitee.ui.util.GEUIUtil
import cn.osc.gitee.util.GEGitRepositoryMapping
import cn.osc.gitee.util.GEHostedRepositoriesManager
import javax.swing.JComponent

internal class GEPRToolWindowTabComponentControllerImpl(
  private val project: Project,
  private val repositoryManager: GEHostedRepositoriesManager,
  private val projectSettings: GiteePullRequestsProjectUISettings,
  private val dataContext: GEPRDataContext,
  private val wrapper: Wrapper,
  private val parentDisposable: Disposable,
  initialView: GEPRToolWindowViewType,
  private val onTitleChange: (@Nls String) -> Unit
) : GEPRToolWindowTabComponentController {

  private val listComponent by lazy { createListPanel() }
  private val createComponentHolder = ClearableLazyValue.create {
    GEPRCreateComponentHolder(ActionManager.getInstance(), project, projectSettings, repositoryManager, dataContext, this,
                              parentDisposable)
  }

  override lateinit var currentView: GEPRToolWindowViewType
  private var currentDisposable: Disposable? = null
  private var currentPullRequest: GEPRIdentifier? = null

  init {
    when (initialView) {
      GEPRToolWindowViewType.NEW -> createPullRequest(false)
      else -> viewList(false)
    }

    DataManager.registerDataProvider(wrapper) { dataId ->
      when {
        GEPRActionKeys.PULL_REQUESTS_TAB_CONTROLLER.`is`(dataId) -> this
        else -> null
      }
    }
  }

  override fun createPullRequest(requestFocus: Boolean) {
    val allRepos = repositoryManager.knownRepositories.map(GEGitRepositoryMapping::repository)
    onTitleChange(GiteeBundle.message("tab.title.pull.requests.new",
                                       GEUIUtil.getRepositoryDisplayName(allRepos,
                                                                         dataContext.repositoryDataService.repositoryCoordinates)))
    currentDisposable?.let { Disposer.dispose(it) }
    currentPullRequest = null
    currentView = GEPRToolWindowViewType.NEW
    wrapper.setContent(createComponentHolder.value.component)
    IJSwingUtilities.updateComponentTreeUI(wrapper)
    if (requestFocus) {
      CollaborationToolsUIUtil.focusPanel(wrapper.targetComponent)
    }
  }

  override fun resetNewPullRequestView() {
    createComponentHolder.value.resetModel()
  }

  override fun viewList(requestFocus: Boolean) {
    val allRepos = repositoryManager.knownRepositories.map(GEGitRepositoryMapping::repository)
    onTitleChange(GiteeBundle.message("tab.title.pull.requests.at",
                                       GEUIUtil.getRepositoryDisplayName(allRepos,
                                                                         dataContext.repositoryDataService.repositoryCoordinates)))
    currentDisposable?.let { Disposer.dispose(it) }
    currentPullRequest = null
    currentView = GEPRToolWindowViewType.LIST
    wrapper.setContent(listComponent)
    IJSwingUtilities.updateComponentTreeUI(wrapper)
    if (requestFocus) {
      CollaborationToolsUIUtil.focusPanel(wrapper.targetComponent)
    }
  }

  override fun refreshList() {
    dataContext.listLoader.reset()
    dataContext.repositoryDataService.resetData()
  }

  override fun viewPullRequest(id: GEPRIdentifier, requestFocus: Boolean, onShown: ((GEPRViewComponentController?) -> Unit)?) {
    onTitleChange(GiteeBundle.message("pull.request.num", id.number))
    if (currentPullRequest != id) {
      currentDisposable?.let { Disposer.dispose(it) }
      currentDisposable = Disposer.newDisposable("Pull request component disposable").also {
        Disposer.register(parentDisposable, it)
      }
      currentPullRequest = id
      currentView = GEPRToolWindowViewType.DETAILS
      val pullRequestComponent = GEPRViewComponentFactory(ActionManager.getInstance(), project, dataContext, this, id,
                                                          currentDisposable!!)
        .create()
      wrapper.setContent(pullRequestComponent)
      wrapper.repaint()
    }
    if (onShown != null) onShown(UIUtil.getClientProperty(wrapper.targetComponent, GEPRViewComponentController.KEY))
    if (requestFocus) {
      CollaborationToolsUIUtil.focusPanel(wrapper.targetComponent)
    }
  }

  override fun openPullRequestTimeline(id: GEPRIdentifier, requestFocus: Boolean) =
    dataContext.filesManager.createAndOpenTimelineFile(id, requestFocus)

  override fun openPullRequestDiff(id: GEPRIdentifier, requestFocus: Boolean) =
    dataContext.filesManager.createAndOpenDiffFile(id, requestFocus)

  private fun createListPanel(): JComponent {
    val listLoader = dataContext.listLoader
    val listModel = CollectionListModel(listLoader.loadedData)
    listLoader.addDataListener(parentDisposable, object : GEListLoader.ListDataListener {
      override fun onDataAdded(startIdx: Int) {
        val loadedData = listLoader.loadedData
        listModel.add(loadedData.subList(startIdx, loadedData.size))
      }

      override fun onDataUpdated(idx: Int) = listModel.setElementAt(listLoader.loadedData[idx], idx)
      override fun onDataRemoved(data: Any) {
        (data as? GEPullRequestShort)?.let { listModel.remove(it) }
      }

      override fun onAllDataRemoved() = listModel.removeAll()
    })

    val list = GEPRListComponentFactory(listModel).create(dataContext.avatarIconsProvider)

    return GEPRListPanelFactory(project,
                                dataContext.repositoryDataService,
                                dataContext.securityService,
                                dataContext.listLoader,
                                dataContext.listUpdatesChecker,
                                dataContext.securityService.account,
                                parentDisposable)
      .create(list, dataContext.avatarIconsProvider)
  }
}