// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.comment.ui

import com.intellij.collaboration.ui.codereview.timeline.comment.CommentInputComponentFactory
import com.intellij.collaboration.ui.codereview.timeline.comment.CommentInputComponentFactory.ActionButtonConfig
import com.intellij.collaboration.ui.codereview.timeline.comment.CommentInputComponentFactory.CancelActionConfig
import com.intellij.collaboration.ui.codereview.timeline.comment.CommentInputComponentFactory.SubmitActionConfig
import com.intellij.collaboration.ui.codereview.timeline.comment.CommentInputComponentFactory.getEditorTextFieldVerticalOffset
import com.intellij.collaboration.ui.codereview.timeline.comment.CommentTextFieldFactory
import com.intellij.collaboration.ui.codereview.timeline.comment.CommentTextFieldModel
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.wm.ex.IdeFocusTraversalPolicy
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import cn.osc.gitee.github.api.data.GHUser
import cn.osc.gitee.github.i18n.GithubBundle
import cn.osc.gitee.github.ui.avatars.GHAvatarIconsProvider
import cn.osc.gitee.github.ui.util.GHUIUtil
import java.awt.Component
import java.awt.Container
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.LayoutFocusTraversalPolicy

class GHCommentTextFieldFactory(private val model: CommentTextFieldModel) {

  fun create(
    @NlsContexts.Tooltip actionName: String = GithubBundle.message("action.comment.text"),
    onCancel: (() -> Unit)? = null
  ): JComponent {
    val commentTextField = CommentTextFieldFactory.create(model, actionName)
    return CommentInputComponentFactory.create(model, commentTextField, createCommentInputConfig(actionName, onCancel))
  }

  fun create(
    avatarIconsProvider: GHAvatarIconsProvider, author: GHUser,
    @NlsContexts.Tooltip actionName: String = GithubBundle.message("action.comment.text"),
    onCancel: (() -> Unit)? = null
  ): JComponent {
    val authorLabel = LinkLabel.create("") {
      BrowserUtil.browse(author.url)
    }.apply {
      icon = avatarIconsProvider.getIcon(author.avatarUrl, GHUIUtil.AVATAR_SIZE)
      isFocusable = true
      border = JBUI.Borders.empty(getEditorTextFieldVerticalOffset() - 2, 0)
      putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true)
    }

    val textField = CommentTextFieldFactory.create(model, actionName)
    val textFieldComponent = CommentInputComponentFactory.create(
      model, textField, createCommentInputConfig(actionName, onCancel)
    )
    return wrapWithAvatar(textFieldComponent, textField, authorLabel)
  }

  private fun createCommentInputConfig(
    @NlsContexts.Tooltip actionName: String,
    onCancel: (() -> Unit)? = null
  ): CommentInputComponentFactory.Config {
    return CommentInputComponentFactory.Config(
      submitConfig = SubmitActionConfig(ActionButtonConfig(actionName)),
      cancelConfig = onCancel?.let { CancelActionConfig(ActionButtonConfig(Messages.getCancelButton()), action = it) }
    )
  }

  private fun wrapWithAvatar(
    component: JComponent,
    commentComponent: JComponent,
    authorLabel: JLabel
  ): JComponent {
    return JPanel(null).apply {
      isOpaque = false
      layout = MigLayout(LC().gridGap("0", "0")
                           .insets("0", "0", "0", "0")
                           .fillX())
      isFocusCycleRoot = true
      isFocusTraversalPolicyProvider = true
      focusTraversalPolicy = object : LayoutFocusTraversalPolicy() {
        override fun getDefaultComponent(aContainer: Container?): Component =
          IdeFocusTraversalPolicy.getPreferredFocusedComponent(commentComponent) ?: super.getDefaultComponent(aContainer)
      }

      add(authorLabel, CC().alignY("top").gapRight("${JBUI.scale(6)}"))
      add(component, CC().grow().pushX())
    }
  }
}
