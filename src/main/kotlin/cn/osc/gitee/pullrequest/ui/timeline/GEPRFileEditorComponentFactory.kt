// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.ui.timeline

import com.intellij.collaboration.async.CompletableFutureUtil.handleOnEdt
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.update.UiNotifyConnector
import net.miginfocom.layout.AC
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import cn.osc.gitee.api.data.GERepositoryPermissionLevel
import cn.osc.gitee.api.data.GEUser
import cn.osc.gitee.api.data.pullrequest.GEPullRequestShort
import cn.osc.gitee.api.data.pullrequest.timeline.GEPRTimelineItem
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.pullrequest.GEPRTimelineFileEditor
import cn.osc.gitee.pullrequest.comment.ui.GECommentTextFieldFactory
import cn.osc.gitee.pullrequest.comment.ui.GECommentTextFieldModel
import cn.osc.gitee.pullrequest.data.GEListLoader
import cn.osc.gitee.pullrequest.data.provider.GEPRCommentsDataProvider
import cn.osc.gitee.pullrequest.data.provider.GEPRDetailsDataProvider
import cn.osc.gitee.pullrequest.data.provider.GEPRReviewDataProvider
import cn.osc.gitee.pullrequest.ui.GEApiLoadingErrorHandler
import cn.osc.gitee.pullrequest.ui.changes.GEPRSuggestedChangeHelper
import cn.osc.gitee.ui.avatars.GEAvatarIconsProvider
import cn.osc.gitee.ui.component.GEHandledErrorPanelModel
import cn.osc.gitee.ui.component.GEHtmlErrorPanel
import cn.osc.gitee.ui.util.GEUIUtil
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

internal class GEPRFileEditorComponentFactory(private val project: Project,
                                              private val editor: GEPRTimelineFileEditor,
                                              currentDetails: GEPullRequestShort) {

  private val uiDisposable = Disposer.newDisposable().also {
    Disposer.register(editor, it)
  }

  private val detailsModel = SingleValueModel(currentDetails)

  private val errorModel = GEHandledErrorPanelModel(GiteeBundle.message("pull.request.timeline.cannot.load"),
                                                    GEApiLoadingErrorHandler(project,
                                                                             editor.securityService.account,
                                                                             editor.timelineLoader::reset))
  private val timelineModel = GEPRTimelineMergingModel()
  private val reviewThreadsModelsProvider = GEPRReviewsThreadsModelsProviderImpl(editor.reviewData, uiDisposable)

  init {
    editor.detailsData.loadDetails(uiDisposable) {
      it.handleOnEdt(uiDisposable) { pr, _ ->
        if (pr != null) detailsModel.value = pr
      }
    }

    editor.timelineLoader.addErrorChangeListener(uiDisposable) {
      errorModel.error = editor.timelineLoader.error
    }
    errorModel.error = editor.timelineLoader.error

    editor.timelineLoader.addDataListener(uiDisposable, object : GEListLoader.ListDataListener {
      override fun onDataAdded(startIdx: Int) {
        val loadedData = editor.timelineLoader.loadedData
        timelineModel.add(loadedData.subList(startIdx, loadedData.size))
      }

      override fun onDataUpdated(idx: Int) {
        val loadedData = editor.timelineLoader.loadedData
        val item = loadedData[idx]
        timelineModel.update(item)
      }

      override fun onDataRemoved(data: Any) {
        if (data !is GEPRTimelineItem) return
        timelineModel.remove(data)
      }

      override fun onAllDataRemoved() = timelineModel.removeAll()
    })
    timelineModel.add(editor.timelineLoader.loadedData)
  }

  fun create(): JComponent {
    val mainPanel = Wrapper()
    DataManager.registerDataProvider(mainPanel, DataProvider {
      if (PlatformDataKeys.UI_DISPOSABLE.`is`(it)) uiDisposable else null
    })

    val header = GEPRTitleComponent.create(project, detailsModel, editor.detailsData)

    val suggestedChangesHelper = GEPRSuggestedChangeHelper(project,
                                                           uiDisposable,
                                                           editor.repositoryDataService.remoteCoordinates.repository,
                                                           editor.reviewData,
                                                           editor.detailsData)
    val timeline = GEPRTimelineComponent(detailsModel,
                                         timelineModel,
                                         createItemComponentFactory(
                                           project,
                                           editor.detailsData, editor.commentsData, editor.reviewData,
                                           reviewThreadsModelsProvider, editor.avatarIconsProvider,
                                           suggestedChangesHelper,
                                           editor.securityService.currentUser
                                         )).apply {
      border = JBUI.Borders.empty(16, 0)
    }
    val errorPanel = GEHtmlErrorPanel.create(errorModel)

    val timelineLoader = editor.timelineLoader
    val loadingIcon = JLabel(AnimatedIcon.Default()).apply {
      border = JBUI.Borders.empty(8, 0)
      isVisible = timelineLoader.loading
    }
    timelineLoader.addLoadingStateChangeListener(uiDisposable) {
      loadingIcon.isVisible = timelineLoader.loading
    }

    val timelinePanel = ScrollablePanel().apply {
      isOpaque = false
      border = JBUI.Borders.empty(24, 20)

      val maxWidth = GEUIUtil.getPRTimelineWidth()

      layout = MigLayout(LC().gridGap("0", "0")
                           .insets("0", "0", "0", "0")
                           .fill()
                           .flowY(),
                         AC().grow().gap("push"))

      add(header, CC().growX().maxWidth("$maxWidth"))
      add(timeline, CC().growX().minWidth(""))

      val fullTimelineWidth = JBUIScale.scale(GEUIUtil.AVATAR_SIZE) + maxWidth

      add(errorPanel, CC().hideMode(2).width("$fullTimelineWidth"))
      add(loadingIcon, CC().hideMode(2).width("$fullTimelineWidth"))

      if (editor.securityService.currentUserHasPermissionLevel(GERepositoryPermissionLevel.READ)) {
        val commentField = createCommentField(editor.commentsData,
                                              editor.avatarIconsProvider,
                                              editor.securityService.currentUser)
        add(commentField, CC().growX().pushX().maxWidth("$fullTimelineWidth"))
      }
    }

    val scrollPane = ScrollPaneFactory.createScrollPane(timelinePanel, true).apply {
      isOpaque = false
      viewport.isOpaque = false
      verticalScrollBar.model.addChangeListener(object : ChangeListener {
        private var firstScroll = true

        override fun stateChanged(e: ChangeEvent) {
          if (firstScroll && verticalScrollBar.value > 0) firstScroll = false
          if (!firstScroll) {
            if (timelineLoader.canLoadMore()) {
              timelineLoader.loadMore()
            }
          }
        }
      })
    }
    UiNotifyConnector.doWhenFirstShown(scrollPane) {
      timelineLoader.loadMore()
    }

    timelineLoader.addDataListener(uiDisposable, object : GEListLoader.ListDataListener {
      override fun onAllDataRemoved() {
        if (scrollPane.isShowing) timelineLoader.loadMore()
      }
    })

    mainPanel.setContent(scrollPane)

    val actionManager = ActionManager.getInstance()
    actionManager.getAction("Gitee.PullRequest.Timeline.Update").registerCustomShortcutSet(scrollPane, uiDisposable)
    val groupId = "Gitee.PullRequest.Timeline.Popup"
    PopupHandler.installPopupMenu(scrollPane, groupId, groupId)

    return mainPanel
  }

  private fun createCommentField(commentService: GEPRCommentsDataProvider,
                                 avatarIconsProvider: GEAvatarIconsProvider,
                                 currentUser: GEUser): JComponent {
    val model = GECommentTextFieldModel(project) {
      commentService.addComment(EmptyProgressIndicator(), it)
    }
    return GECommentTextFieldFactory(model).create(avatarIconsProvider, currentUser)
  }

  private fun createItemComponentFactory(project: Project,
                                         detailsDataProvider: GEPRDetailsDataProvider,
                                         commentsDataProvider: GEPRCommentsDataProvider,
                                         reviewDataProvider: GEPRReviewDataProvider,
                                         reviewThreadsModelsProvider: GEPRReviewsThreadsModelsProvider,
                                         avatarIconsProvider: GEAvatarIconsProvider,
                                         suggestedChangeHelper: GEPRSuggestedChangeHelper,
                                         currentUser: GEUser)
    : GEPRTimelineItemComponentFactory {

    val selectInToolWindowHelper = GEPRSelectInToolWindowHelper(project, detailsModel.value)
    val diffFactory = GEPRReviewThreadDiffComponentFactory(project, EditorFactory.getInstance())
    val eventsFactory = GEPRTimelineEventComponentFactoryImpl(avatarIconsProvider)
    return GEPRTimelineItemComponentFactory(
      project,
      detailsDataProvider, commentsDataProvider, reviewDataProvider, avatarIconsProvider, reviewThreadsModelsProvider,
      diffFactory,
      eventsFactory, selectInToolWindowHelper,
      suggestedChangeHelper, currentUser
    )
  }
}