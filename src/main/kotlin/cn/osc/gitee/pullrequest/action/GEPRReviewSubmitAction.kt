// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.action

import com.intellij.collaboration.async.CompletableFutureUtil.errorOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.util.ui.InlineIconButton
import com.intellij.icons.AllIcons
import com.intellij.ide.ui.laf.darcula.ui.DarculaButtonUI
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.actions.IncrementalFindAction
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.ui.ComponentContainer
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.EditorTextField
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.components.panels.HorizontalBox
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JButtonAction
import com.intellij.util.ui.UIUtil
import icons.CollaborationToolsIcons
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import cn.osc.gitee.api.data.GEPullRequestReviewEvent
import cn.osc.gitee.api.data.pullrequest.GEPullRequestPendingReview
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.pullrequest.data.provider.GEPRReviewDataProvider
import cn.osc.gitee.ui.component.GEHtmlErrorPanel
import cn.osc.gitee.ui.component.GESimpleErrorPanelModel
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.ActionListener
import javax.swing.*

class GEPRReviewSubmitAction : JButtonAction(StringUtil.ELLIPSIS, GiteeBundle.message("pull.request.review.submit.action.description")) {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  override fun update(e: AnActionEvent) {
    val dataProvider = e.getData(GEPRActionKeys.PULL_REQUEST_DATA_PROVIDER)
    if (dataProvider == null) {
      e.presentation.isEnabledAndVisible = false
      return
    }

    val reviewData = dataProvider.reviewData
    val details = dataProvider.detailsData.loadedDetails
    e.presentation.isVisible = true
    val pendingReviewFuture = reviewData.loadPendingReview()
    e.presentation.isEnabled = pendingReviewFuture.isDone && details != null
    e.presentation.putClientProperty(PROP_PREFIX, getPrefix(e.place))

    if (e.presentation.isEnabledAndVisible) {
      val review = try {
        pendingReviewFuture.getNow(null)
      }
      catch (e: Exception) {
        null
      }
      val pendingReview = review != null
      val comments = review?.comments?.totalCount

      e.presentation.text = getText(comments)
      e.presentation.putClientProperty(DarculaButtonUI.DEFAULT_STYLE_KEY, pendingReview)
    }
  }

  private fun getPrefix(place: String) = if (place == ActionPlaces.DIFF_TOOLBAR) GiteeBundle.message("pull.request.review.submit")
  else GiteeBundle.message("pull.request.review.submit.review")

  @NlsSafe
  private fun getText(pendingComments: Int?): String {
    val builder = StringBuilder()
    if (pendingComments != null) builder.append(" ($pendingComments)")
    builder.append(StringUtil.ELLIPSIS)
    return builder.toString()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val dataProvider = e.getRequiredData(GEPRActionKeys.PULL_REQUEST_DATA_PROVIDER)
    val details = dataProvider.detailsData.loadedDetails ?: return
    val reviewDataProvider = dataProvider.reviewData
    val pendingReviewFuture = reviewDataProvider.loadPendingReview()
    if (!pendingReviewFuture.isDone) return
    val pendingReview = try {
      pendingReviewFuture.getNow(null)
    }
    catch (e: Exception) {
      null
    }
    val parentComponent = e.presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY) ?: return

    var cancelRunnable: (() -> Unit)? = null
    val cancelActionListener = ActionListener {
      cancelRunnable?.invoke()
    }

    val container = createPopupComponent(reviewDataProvider, reviewDataProvider.submitReviewCommentDocument,
                                         cancelActionListener, pendingReview,
                                         details.viewerDidAuthor)
    val popup = JBPopupFactory.getInstance()
      .createComponentPopupBuilder(container.component, container.preferredFocusableComponent)
      .setFocusable(true)
      .setRequestFocus(true)
      .setResizable(true)
      .createPopup()

    cancelRunnable = { popup.cancel() }

    popup.showUnderneathOf(parentComponent)
  }

  private fun createPopupComponent(reviewDataProvider: GEPRReviewDataProvider,
                                   document: Document,
                                   cancelActionListener: ActionListener,
                                   pendingReview: GEPullRequestPendingReview?,
                                   viewerIsAuthor: Boolean): ComponentContainer {
    return object : ComponentContainer {

      private val editor = createEditor(document)
      private val errorModel = GESimpleErrorPanelModel(GiteeBundle.message("pull.request.review.submit.error"))

      private val approveButton = if (!viewerIsAuthor) JButton(GiteeBundle.message("pull.request.review.submit.approve.button")).apply {
        addActionListener(createSubmitButtonActionListener(GEPullRequestReviewEvent.APPROVE))
      }
      else null

      private val rejectButton = if (!viewerIsAuthor) JButton(GiteeBundle.message("pull.request.review.submit.request.changes")).apply {
        addActionListener(createSubmitButtonActionListener(GEPullRequestReviewEvent.REQUEST_CHANGES))
      }
      else null

      private val commentButton = JButton(GiteeBundle.message("pull.request.review.submit.comment.button")).apply {
        toolTipText = GiteeBundle.message("pull.request.review.submit.comment.description")
        addActionListener(createSubmitButtonActionListener(GEPullRequestReviewEvent.COMMENT))
      }

      private fun createSubmitButtonActionListener(event: GEPullRequestReviewEvent): ActionListener = ActionListener { e ->
        editor.isEnabled = false
        approveButton?.isEnabled = false
        rejectButton?.isEnabled = false
        commentButton.isEnabled = false
        discardButton?.isEnabled = false

        val reviewId = pendingReview?.id
        if (reviewId == null) {
          reviewDataProvider.createReview(EmptyProgressIndicator(), event, editor.text)
        }
        else {
          reviewDataProvider.submitReview(EmptyProgressIndicator(), reviewId, event, editor.text)
        }.successOnEdt {
          cancelActionListener.actionPerformed(e)
          runWriteAction { document.setText("") }
        }.errorOnEdt {
          errorModel.error = it
          editor.isEnabled = true
          approveButton?.isEnabled = true
          rejectButton?.isEnabled = true
          commentButton.isEnabled = true
          discardButton?.isEnabled = true
        }
      }

      private val discardButton: InlineIconButton?

      init {
        discardButton = pendingReview?.let { review ->
          val button = InlineIconButton(icon = CollaborationToolsIcons.Delete, hoveredIcon = CollaborationToolsIcons.DeleteHovered,
                                        tooltip = GiteeBundle.message("pull.request.discard.pending.comments"))
          button.actionListener = ActionListener {
            if (MessageDialogBuilder.yesNo(GiteeBundle.message("pull.request.discard.pending.comments.dialog.title"),
                                           GiteeBundle.message("pull.request.discard.pending.comments.dialog.msg")).ask(button)) {
              reviewDataProvider.deleteReview(EmptyProgressIndicator(), review.id)
            }
          }
          button
        }
      }

      override fun getComponent(): JComponent {
        val titleLabel = JLabel(GiteeBundle.message("pull.request.review.submit.review")).apply {
          font = font.deriveFont(font.style or Font.BOLD)
        }
        val titlePanel = HorizontalBox().apply {
          border = JBUI.Borders.empty(4, 4, 4, 4)

          add(titleLabel)
          if (pendingReview != null) {
            val commentsCount = pendingReview.comments.totalCount!!
            add(Box.createRigidArea(JBDimension(5, 0)))
            add(JLabel(GiteeBundle.message("pull.request.review.pending.comments.count", commentsCount))).apply {
              foreground = UIUtil.getContextHelpForeground()
            }
          }
          add(Box.createHorizontalGlue())
          discardButton?.let { add(it) }
          add(InlineIconButton(AllIcons.Actions.Close, AllIcons.Actions.CloseHovered).apply {
            actionListener = cancelActionListener
          })
        }

        val errorPanel = GEHtmlErrorPanel.create(errorModel, SwingConstants.LEFT).apply {
          border = JBUI.Borders.empty(4)
        }

        val buttonsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
          border = JBUI.Borders.empty(4)

          if (!viewerIsAuthor) add(approveButton)
          if (!viewerIsAuthor) add(rejectButton)
          add(commentButton)
        }

        return JPanel(MigLayout(LC().gridGap("0", "0")
                                  .insets("0", "0", "0", "0")
                                  .fill().flowY().noGrid())).apply {
          isOpaque = false
          preferredSize = JBDimension(450, 165)

          add(titlePanel, CC().growX())
          add(editor, CC().growX().growY()
            .gap("0", "0", "0", "0"))
          add(errorPanel, CC().minHeight("${JBUIScale.scale(32)}").growY().growPrioY(0).hideMode(3)
            .gap("0", "0", "0", "0"))
          add(buttonsPanel, CC().alignX("right"))
        }
      }

      private fun createEditor(document: Document) = EditorTextField(document, null, FileTypes.PLAIN_TEXT).apply {
        setOneLineMode(false)
        putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true)
        setPlaceholder(GiteeBundle.message("pull.request.review.comment.empty.text"))
        addSettingsProvider {
          it.settings.isUseSoftWraps = true
          it.setVerticalScrollbarVisible(true)
          it.scrollPane.border = IdeBorderFactory.createBorder(SideBorder.TOP or SideBorder.BOTTOM)
          it.scrollPane.viewportBorder = JBUI.Borders.emptyLeft(4)
          it.putUserData(IncrementalFindAction.SEARCH_DISABLED, true)
        }
      }


      override fun getPreferredFocusableComponent() = editor

      override fun dispose() {}
    }
  }

  override fun updateButtonFromPresentation(button: JButton, presentation: Presentation) {
    super.updateButtonFromPresentation(button, presentation)
    val prefix = presentation.getClientProperty(PROP_PREFIX) as? String ?: GiteeBundle.message("pull.request.review.submit.review")
    button.text = prefix + presentation.text
    UIUtil.putClientProperty(button, DarculaButtonUI.DEFAULT_STYLE_KEY, presentation.getClientProperty(DarculaButtonUI.DEFAULT_STYLE_KEY))
  }

  companion object {
    private const val PROP_PREFIX = "PREFIX"
  }
}