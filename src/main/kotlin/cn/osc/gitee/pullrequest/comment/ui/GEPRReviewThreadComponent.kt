// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.comment.ui

import com.intellij.collaboration.async.CompletableFutureUtil.handleOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.util.ui.InlineIconButton
import com.intellij.collaboration.ui.codereview.ToggleableContainer
import com.intellij.collaboration.ui.codereview.comment.RoundedPanel
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.ui.ClickListener
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.HorizontalBox
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.PathUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import cn.osc.gitee.api.data.GEUser
import cn.osc.gitee.api.data.pullrequest.GEPullRequestReviewCommentState
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.pullrequest.data.provider.GEPRReviewDataProvider
import cn.osc.gitee.pullrequest.ui.changes.GEPRSuggestedChangeHelper
import cn.osc.gitee.pullrequest.ui.timeline.GEPRReviewThreadDiffComponentFactory
import cn.osc.gitee.pullrequest.ui.timeline.GEPRSelectInToolWindowHelper
import cn.osc.gitee.ui.avatars.GEAvatarIconsProvider
import cn.osc.gitee.ui.util.GEUIUtil
import java.awt.Cursor
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import javax.swing.*
import kotlin.properties.Delegates

object GEPRReviewThreadComponent {

  fun create(project: Project,
             thread: GEPRReviewThreadModel,
             reviewDataProvider: GEPRReviewDataProvider,
             avatarIconsProvider: GEAvatarIconsProvider,
             suggestedChangeHelper: GEPRSuggestedChangeHelper,
             currentUser: GEUser): JComponent {
    val panel = JPanel(VerticalLayout(12)).apply {
      isOpaque = false
    }
    panel.add(GEPRReviewThreadCommentsPanel.create(thread, GEPRReviewCommentComponent.factory(project, thread,
                                                                                              reviewDataProvider, avatarIconsProvider,
                                                                                              suggestedChangeHelper)))

    if (reviewDataProvider.canComment()) {
      panel.add(getThreadActionsComponent(project, reviewDataProvider, thread, avatarIconsProvider, currentUser))
    }
    return panel
  }

  fun createWithDiff(project: Project,
                     thread: GEPRReviewThreadModel,
                     reviewDataProvider: GEPRReviewDataProvider,
                     avatarIconsProvider: GEAvatarIconsProvider,
                     diffComponentFactory: GEPRReviewThreadDiffComponentFactory,
                     selectInToolWindowHelper: GEPRSelectInToolWindowHelper,
                     suggestedChangeHelper: GEPRSuggestedChangeHelper,
                     currentUser: GEUser): JComponent {

    val collapseButton = InlineIconButton(AllIcons.General.CollapseComponent, AllIcons.General.CollapseComponentHover,
                                          tooltip = GiteeBundle.message("pull.request.timeline.review.thread.collapse"))
    val expandButton = InlineIconButton(AllIcons.General.ExpandComponent, AllIcons.General.ExpandComponentHover,
                                        tooltip = GiteeBundle.message("pull.request.timeline.review.thread.expand"))

    val contentPanel = RoundedPanel(VerticalLayout(4), 8).apply {
      isOpaque = false
      add(createFileName(thread, selectInToolWindowHelper, collapseButton, expandButton))
    }

    val commentPanel = JPanel(VerticalLayout(4)).apply {
      isOpaque = false
    }

    object : CollapseController(thread, contentPanel, commentPanel, collapseButton, expandButton) {

      override fun createDiffAndCommentsPanels(): Pair<JComponent, JComponent> {
        val diffComponent = diffComponentFactory.createComponent(thread.diffHunk, thread.startLine).apply {
          border = IdeBorderFactory.createBorder(SideBorder.TOP)
        }

        val commentsComponent = JPanel(VerticalLayout(12)).apply {
          isOpaque = false
          val reviewCommentComponent = GEPRReviewCommentComponent.factory(project, thread,
                                                                          reviewDataProvider, avatarIconsProvider,
                                                                          suggestedChangeHelper,
                                                                          false)
          add(GEPRReviewThreadCommentsPanel.create(thread, reviewCommentComponent))

          if (reviewDataProvider.canComment()) {
            add(getThreadActionsComponent(project, reviewDataProvider, thread, avatarIconsProvider, currentUser))
          }
        }
        return diffComponent to commentsComponent
      }
    }


    return JPanel(VerticalLayout(4)).apply {
      isOpaque = false
      add(contentPanel)
      add(commentPanel)
    }
  }

  private abstract class CollapseController(private val thread: GEPRReviewThreadModel,
                                            private val contentPanel: JPanel,
                                            private val commentPanel: JPanel,
                                            private val collapseButton: InlineIconButton,
                                            private val expandButton: InlineIconButton) {

    private val collapseModel = SingleValueModel(true)
    private var childPanels by Delegates.observable<Pair<JComponent, JComponent>?>(null) { _, oldValue, newValue ->
      var revalidate = false
      if (oldValue != null) {
        contentPanel.remove(oldValue.first)
        commentPanel.remove(oldValue.second)
        revalidate = true
      }
      if (newValue != null) {
        contentPanel.add(newValue.first)
        commentPanel.add(newValue.second)
      }
      if (revalidate) {
        contentPanel.revalidate()
        commentPanel.revalidate()
      }
      else {
        contentPanel.validate()
        commentPanel.validate()
      }
      contentPanel.repaint()
      commentPanel.repaint()
    }

    init {
      collapseButton.actionListener = ActionListener { collapseModel.value = true }
      expandButton.actionListener = ActionListener { collapseModel.value = false }
      collapseModel.addListener { update() }
      thread.addAndInvokeStateChangeListener(::update)
    }

    private fun update() {
      val shouldBeVisible = !thread.isResolved || !collapseModel.value
      if (shouldBeVisible) {
        if (childPanels == null) {
          childPanels = createDiffAndCommentsPanels()
        }
      }
      else {
        childPanels = null
      }

      collapseButton.isVisible = thread.isResolved && !collapseModel.value
      expandButton.isVisible = thread.isResolved && collapseModel.value
    }

    abstract fun createDiffAndCommentsPanels(): Pair<JComponent, JComponent>
  }

  private fun createFileName(thread: GEPRReviewThreadModel,
                             selectInToolWindowHelper: GEPRSelectInToolWindowHelper,
                             collapseButton: InlineIconButton,
                             expandButton: InlineIconButton): JComponent {
    val name = PathUtil.getFileName(thread.filePath)
    val path = PathUtil.getParentPath(thread.filePath)
    val fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(name)

    val nameLabel = JLabel(name, fileType.icon, SwingConstants.LEFT).apply {
      cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
      object : ClickListener() {
        override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
          selectInToolWindowHelper.selectChange(thread.commit?.oid, thread.filePath)
          return true
        }
      }.installOn(this)
    }

    val outdatedLabel = JBLabel(" ${GiteeBundle.message("pull.request.review.thread.outdated")} ", UIUtil.ComponentStyle.SMALL).apply {
      foreground = UIUtil.getContextHelpForeground()
      background = UIUtil.getPanelBackground()
    }.andOpaque()

    val resolvedLabel = JBLabel(" ${GiteeBundle.message("pull.request.review.comment.resolved")} ", UIUtil.ComponentStyle.SMALL).apply {
      foreground = UIUtil.getContextHelpForeground()
      background = UIUtil.getPanelBackground()
    }.andOpaque()


    thread.addAndInvokeStateChangeListener {
      outdatedLabel.isVisible = thread.isOutdated
      resolvedLabel.isVisible = thread.isResolved
    }

    return NonOpaquePanel(MigLayout(LC().insets("0").gridGap("${JBUIScale.scale(5)}", "0").fill().noGrid())).apply {
      border = JBUI.Borders.empty(10)

      add(nameLabel)

      if (!path.isBlank()) add(JLabel(path).apply {
        foreground = UIUtil.getContextHelpForeground()
      })

      add(outdatedLabel, CC().hideMode(3))
      add(resolvedLabel, CC().hideMode(3))

      add(collapseButton, CC().hideMode(3))
      add(expandButton, CC().hideMode(3))
    }
  }

  private fun getThreadActionsComponent(
    project: Project,
    reviewDataProvider: GEPRReviewDataProvider,
    thread: GEPRReviewThreadModel,
    avatarIconsProvider: GEAvatarIconsProvider,
    currentUser: GEUser
  ): JComponent {
    val toggleModel = SingleValueModel(false)
    val textFieldModel = GECommentTextFieldModel(project) { text ->
      reviewDataProvider.addComment(EmptyProgressIndicator(), thread.getElementAt(0).id, text).successOnEdt {
        thread.addComment(GEPRReviewCommentModel.convert(it))
        toggleModel.value = false
      }
    }

    val toggleReplyLink = LinkLabel<Any>(GiteeBundle.message("pull.request.review.thread.reply"), null) { _, _ ->
      toggleModel.value = true
    }.apply {
      isFocusable = true
    }

    val resolveLink = LinkLabel<Any>(GiteeBundle.message("pull.request.review.thread.resolve"), null).apply {
      isFocusable = true
    }.also {
      it.setListener({ _, _ ->
                       it.isEnabled = false
                       reviewDataProvider.resolveThread(EmptyProgressIndicator(), thread.id).handleOnEdt { _, _ ->
                         it.isEnabled = true
                       }
                     }, null)
    }

    val unresolveLink = LinkLabel<Any>(GiteeBundle.message("pull.request.review.thread.unresolve"), null).apply {
      isFocusable = true
    }.also {
      it.setListener({ _, _ ->
                       it.isEnabled = false
                       reviewDataProvider.unresolveThread(EmptyProgressIndicator(), thread.id).handleOnEdt { _, _ ->
                         it.isEnabled = true
                       }
                     }, null)
    }

    val content = ToggleableContainer.create(
      toggleModel,
      { createThreadActionsComponent(thread, toggleReplyLink, resolveLink, unresolveLink) },
      {
        GECommentTextFieldFactory(textFieldModel).create(avatarIconsProvider, currentUser,
                                                         GiteeBundle.message(
                                                               "pull.request.review.thread.reply"),
                                                         onCancel = { toggleModel.value = false })
      }
    )
    return JPanel().apply {
      isOpaque = false
      layout = MigLayout(LC().insets("0"))
      add(content, CC().width("${GEUIUtil.getPRTimelineWidth() + JBUIScale.scale(GEUIUtil.AVATAR_SIZE)}"))
    }
  }

  private fun createThreadActionsComponent(model: GEPRReviewThreadModel,
                                           toggleReplyLink: LinkLabel<Any>,
                                           resolveLink: LinkLabel<Any>,
                                           unresolveLink: LinkLabel<Any>): JComponent {
    fun update() {
      resolveLink.isVisible = model.state != GEPullRequestReviewCommentState.PENDING && !model.isResolved
      unresolveLink.isVisible = model.state != GEPullRequestReviewCommentState.PENDING && model.isResolved
    }

    model.addAndInvokeStateChangeListener(::update)

    return HorizontalBox().apply {
      isOpaque = false
      border = JBUI.Borders.empty(6, 34, 6, 0)

      add(toggleReplyLink)
      add(Box.createHorizontalStrut(JBUIScale.scale(8)))
      add(resolveLink)
      add(unresolveLink)
    }
  }
}