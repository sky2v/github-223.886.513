// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.ui.timeline

import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import cn.osc.gitee.api.data.pullrequest.GEPullRequestShort
import cn.osc.gitee.pullrequest.data.provider.GEPRDetailsDataProvider
import cn.osc.gitee.pullrequest.ui.GEEditableHtmlPaneHandle
import cn.osc.gitee.pullrequest.ui.GETextActions
import cn.osc.gitee.pullrequest.ui.details.GEPRDetailsModel
import cn.osc.gitee.ui.util.GEUIUtil
import cn.osc.gitee.ui.util.HtmlEditorPane
import java.awt.event.ActionListener
import javax.swing.JComponent
import javax.swing.JLabel

internal object GEPRTitleComponent {

  fun create(project: Project, model: SingleValueModel<GEPullRequestShort>, detailsDataProvider: GEPRDetailsDataProvider): JComponent {
    val icon = JLabel()
    val titlePane = HtmlEditorPane().apply {
      font = font.deriveFont((font.size * 1.5).toFloat())
    }
    val editButton = GETextActions.createEditButton()

    model.addAndInvokeListener {
      icon.icon = GEUIUtil.getPullRequestStateIcon(model.value.state, model.value.isDraft)
      titlePane.setBody(getTitleBody(model.value.title, model.value.number.toString()))
    }

    val fieldPanel = layout(icon, titlePane, editButton)

    val paneHandle = GEEditableHtmlPaneHandle(project, fieldPanel, { model.value.title }) { newText ->
      detailsDataProvider.updateDetails(EmptyProgressIndicator(), title = newText)
        .successOnEdt { titlePane.setBody(getTitleBody(newText, model.value.number.toString())) }
    }

    editButton.actionListener = ActionListener {
      paneHandle.showAndFocusEditor()
    }

    editButton.isVisible = model.value.viewerCanUpdate
    return paneHandle.panel
  }

  fun create(detailsModel: GEPRDetailsModel): JComponent {
    val icon = JLabel()
    val title = HtmlEditorPane().apply {
      font = font.deriveFont((font.size * 1.2).toFloat())
    }

    detailsModel.addAndInvokeDetailsChangedListener {
      icon.icon = GEUIUtil.getPullRequestStateIcon(detailsModel.state, detailsModel.isDraft)
      title.setBody(getTitleBody(detailsModel.title, detailsModel.number))
    }

    return layout(icon, title)
  }

  @NlsSafe
  private fun getTitleBody(@NlsSafe title: String, @NlsSafe number: String): String {
    val contextHelpColorText = ColorUtil.toHtmlColor(UIUtil.getContextHelpForeground())
    //language=html
    return HtmlBuilder()
      .append(title)
      .nbsp()
      .append(HtmlChunk.span("color: $contextHelpColorText").addText("#${number}"))
      .toString()
  }

  private fun layout(icon: JLabel, title: JComponent, editButton: JComponent? = null): JComponent {
    return NonOpaquePanel(MigLayout(LC().insets("0").gridGap("0", "0").noGrid())).apply {
      add(icon, CC().gapRight("${JBUIScale.scale(4)}"))
      add(title, CC())
      if (editButton != null) add(editButton, CC().gapLeft("${JBUIScale.scale(12)}").hideMode(3))
    }
  }
}