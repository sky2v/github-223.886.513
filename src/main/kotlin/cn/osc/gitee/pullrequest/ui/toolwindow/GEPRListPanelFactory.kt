// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.pullrequest.ui.toolwindow

import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBList
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.StatusText
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.scroll.BoundedRangeModelThresholdListener
import com.intellij.vcs.log.ui.frame.ProgressStripe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import cn.osc.gitee.api.data.pullrequest.GEPullRequestShort
import cn.osc.gitee.authentication.accounts.GiteeAccount
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.pullrequest.action.GEPRActionKeys
import cn.osc.gitee.pullrequest.data.GEListLoader
import cn.osc.gitee.pullrequest.data.GEPRListLoader
import cn.osc.gitee.pullrequest.data.GEPRListUpdatesChecker
import cn.osc.gitee.pullrequest.data.service.GEPRRepositoryDataService
import cn.osc.gitee.pullrequest.data.service.GEPRSecurityService
import cn.osc.gitee.pullrequest.ui.GEApiLoadingErrorHandler
import cn.osc.gitee.ui.avatars.GEAvatarIconsProvider
import cn.osc.gitee.ui.component.GEHandledErrorPanelModel
import cn.osc.gitee.ui.component.GEHtmlErrorPanel
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.event.ChangeEvent

internal class GEPRListPanelFactory(private val project: Project,
                                    private val repositoryDataService: GEPRRepositoryDataService,
                                    private val securityService: GEPRSecurityService,
                                    private val listLoader: GEPRListLoader,
                                    private val listUpdatesChecker: GEPRListUpdatesChecker,
                                    private val account: GiteeAccount,
                                    private val disposable: Disposable) {

  private val scope = MainScope().also { Disposer.register(disposable) { it.cancel() } }

  fun create(list: JBList<GEPullRequestShort>, avatarIconsProvider: GEAvatarIconsProvider): JComponent {

    val actionManager = ActionManager.getInstance()

    val historyModel = GEPRSearchHistoryModel(project.service<GEPRListPersistentSearchHistory>())
    val searchVm = GEPRSearchPanelViewModel(scope, repositoryDataService, historyModel, securityService.currentUser)
    scope.launch {
      searchVm.searchState.collectLatest {
        listLoader.searchQuery = it.toQuery()
      }
    }

    val repository = repositoryDataService.repositoryCoordinates.repositoryPath.repository
    ListEmptyTextController(scope, listLoader, searchVm, list.emptyText, repository, disposable)

    val searchPanel = GEPRSearchPanelFactory(searchVm, avatarIconsProvider).create(scope)

    val outdatedStatePanel = JPanel(FlowLayout(FlowLayout.LEFT, JBUIScale.scale(5), 0)).apply {
      background = UIUtil.getPanelBackground()
      border = JBUI.Borders.empty(4, 0)
      add(JLabel(GiteeBundle.message("pull.request.list.outdated")))
      add(ActionLink(GiteeBundle.message("pull.request.list.refresh")) {
        listLoader.reset()
      })

      isVisible = false
    }
    OutdatedPanelController(listLoader, listUpdatesChecker, outdatedStatePanel, disposable)

    val errorHandler = GEApiLoadingErrorHandler(project, account) {
      listLoader.reset()
    }
    val errorModel = GEHandledErrorPanelModel(GiteeBundle.message("pull.request.list.cannot.load"), errorHandler).apply {
      error = listLoader.error
    }
    listLoader.addErrorChangeListener(disposable) {
      errorModel.error = listLoader.error
    }
    val errorPane = GEHtmlErrorPanel.create(errorModel)

    val controlsPanel = JPanel(VerticalLayout(0)).apply {
      isOpaque = false
      add(searchPanel)
      add(outdatedStatePanel)
      add(errorPane)
    }
    val listLoaderPanel = createListLoaderPanel(listLoader, list, disposable)
    return JBUI.Panels.simplePanel(listLoaderPanel).addToTop(controlsPanel).andTransparent().also {
      DataManager.registerDataProvider(it) { dataId ->
        if (GEPRActionKeys.SELECTED_PULL_REQUEST.`is`(dataId)) {
          if (list.isSelectionEmpty) null else list.selectedValue
        }
        else null
      }
      actionManager.getAction("Gitee.PullRequest.List.Reload").registerCustomShortcutSet(it, disposable)
    }
  }

  private fun createListLoaderPanel(loader: GEListLoader<*>, list: JComponent, disposable: Disposable): JComponent {

    val scrollPane = ScrollPaneFactory.createScrollPane(list, true).apply {
      isOpaque = false
      viewport.isOpaque = false
      horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
      verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED

      val model = verticalScrollBar.model
      val listener = object : BoundedRangeModelThresholdListener(model, 0.7f) {
        override fun onThresholdReached() {
          if (!loader.loading && loader.canLoadMore()) {
            loader.loadMore()
          }
        }
      }
      model.addChangeListener(listener)
      loader.addLoadingStateChangeListener(disposable) {
        if (!loader.loading) listener.stateChanged(ChangeEvent(loader))
      }
    }
    loader.addDataListener(disposable, object : GEListLoader.ListDataListener {
      override fun onAllDataRemoved() {
        if (scrollPane.isShowing) loader.loadMore()
      }
    })
    val progressStripe = ProgressStripe(scrollPane, disposable,
                                        ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS).apply {
      if (loader.loading) startLoadingImmediately() else stopLoading()
    }
    loader.addLoadingStateChangeListener(disposable) {
      if (loader.loading) progressStripe.startLoading() else progressStripe.stopLoading()
    }
    return progressStripe
  }

  private class ListEmptyTextController(scope: CoroutineScope,
                                        private val listLoader: GEListLoader<*>,
                                        private val searchVm: GEPRSearchPanelViewModel,
                                        private val emptyText: StatusText,
                                        private val repository: String,
                                        listenersDisposable: Disposable) {
    init {
      listLoader.addLoadingStateChangeListener(listenersDisposable, ::update)
      scope.launch {
        searchVm.searchState.collect {
          update()
        }
      }
    }

    private fun update() {
      emptyText.clear()
      if (listLoader.loading || listLoader.error != null) return

      val search = searchVm.searchState.value
      if (search.filterCount == 0) {
        emptyText.appendText(GiteeBundle.message("pull.request.list.nothing.loaded", repository))
      }
      else {
        emptyText
          .appendText(GiteeBundle.message("pull.request.list.no.matches"))
          .appendSecondaryText(GiteeBundle.message("pull.request.list.filters.clear"), SimpleTextAttributes.LINK_ATTRIBUTES) {
            searchVm.searchState.value = GEPRListSearchValue.EMPTY
          }
      }
    }
  }

  private class OutdatedPanelController(private val listLoader: GEListLoader<*>,
                                        private val listChecker: GEPRListUpdatesChecker,
                                        private val panel: JPanel,
                                        listenersDisposable: Disposable) {
    init {
      listLoader.addLoadingStateChangeListener(listenersDisposable, ::update)
      listLoader.addErrorChangeListener(listenersDisposable, ::update)
      listChecker.addOutdatedStateChangeListener(listenersDisposable, ::update)
    }

    private fun update() {
      panel.isVisible = listChecker.outdated && (!listLoader.loading && listLoader.error == null)
    }
  }
}