// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.ui

import com.intellij.collaboration.ui.CollaborationToolsUIUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.ui.AnimatedIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.SingleComponentCenteringLayout
import com.intellij.util.ui.UIUtil
import com.intellij.vcs.log.ui.frame.ProgressStripe
import org.jetbrains.annotations.Nls
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.ui.component.GEHtmlErrorPanel
import cn.osc.gitee.ui.util.HtmlEditorPane
import com.intellij.collaboration.ui.SingleValueModel
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.properties.Delegates

class GELoadingPanelFactory<T>(private val model: GESimpleLoadingModel<T>,
                               @Nls(capitalization = Nls.Capitalization.Sentence) private val notLoadingText: String? = null,
                               @Nls(capitalization = Nls.Capitalization.Sentence) private val errorPrefix: String =
                                 GiteeBundle.message("cannot.load.data"),
                               private val errorHandler: GELoadingErrorHandler? = null) {

  private val contentListeners = mutableListOf<(JComponent) -> Unit>()

  fun withContentListener(listener: (JComponent) -> Unit): GELoadingPanelFactory<T> {
    contentListeners.add(listener)
    return this
  }

  fun createWithUpdatesStripe(parentDisposable: Disposable, contentFactory: (JPanel, SingleValueModel<T>) -> JComponent): JComponent {
    val panel = JPanel(null).apply {
      isOpaque = false
    }
    object : ContentController<T>(model, panel, notLoadingText, errorPrefix, errorHandler, contentListeners.toList()) {
      override fun createContent(parentPanel: JPanel, valueModel: SingleValueModel<T>): JComponent {
        val stripe = ProgressStripe(contentFactory(parentPanel, valueModel), parentDisposable,
                                    ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS).apply {
          isOpaque = false
        }

        fun updateStripe() {
          if (model.resultAvailable) {
            if (model.loading) stripe.startLoadingImmediately() else stripe.stopLoading()
          }
          else stripe.stopLoading()
        }
        model.addStateChangeListener(object : GELoadingModel.StateChangeListener {
          override fun onLoadingStarted() = updateStripe()
          override fun onLoadingCompleted() = updateStripe()
        })
        updateStripe()
        return stripe
      }
    }
    return panel
  }

  fun create(contentFactory: (JPanel, T) -> JComponent): JComponent {
    val panel = JPanel(null).apply {
      isOpaque = false
    }
    object : ContentController<T>(model, panel, notLoadingText, errorPrefix, errorHandler, contentListeners.toList()) {
      override fun createContent(parentPanel: JPanel, valueModel: SingleValueModel<T>) =
        contentFactory(parentPanel, valueModel.value)
    }
    return panel
  }

  fun createWithModel(contentFactory: (JPanel, SingleValueModel<T>) -> JComponent): JComponent {
    val panel = JPanel(null).apply {
      isOpaque = false
    }
    object : ContentController<T>(model, panel, notLoadingText, errorPrefix, errorHandler, contentListeners.toList()) {
      override fun createContent(parentPanel: JPanel, valueModel: SingleValueModel<T>) = contentFactory(parentPanel, valueModel)
    }
    return panel
  }

  companion object {
    private abstract class ContentController<T>(private val model: GESimpleLoadingModel<T>, private val panel: JPanel,
                                                private val notLoadingText: String?,
                                                private val errorPrefix: String,
                                                private val errorHandler: GELoadingErrorHandler?,
                                                private val contentListeners: List<(JComponent) -> Unit>) {

      private var valueModel: SingleValueModel<T>? = null
      private var content by Delegates.observable<JComponent?>(null) { _, oldValue, newValue ->
        val wasFocused = UIUtil.isFocusAncestor(panel)
        if (oldValue !== newValue) {
          if (oldValue != null) panel.remove(oldValue)
          if (newValue != null) {
            panel.add(newValue, BorderLayout.CENTER)
            contentListeners.forEach { it(newValue) }
          }
          panel.validate()
          panel.repaint()
          if (wasFocused) {
            CollaborationToolsUIUtil.focusPanel(panel)
          }
        }
      }

      init {
        panel.layout = BorderLayout()
        model.addStateChangeListener(object : GELoadingModel.StateChangeListener {
          override fun onLoadingStarted() = update()
          override fun onLoadingCompleted() = update()
        })
        update()
      }

      private fun update() {
        if (model.resultAvailable) {
          @Suppress("UNCHECKED_CAST")
          val result = model.result as T
          var currentValueModel = valueModel
          if (currentValueModel != null) {
            currentValueModel.value = result
            return
          }
          currentValueModel = SingleValueModel(result)
          valueModel = currentValueModel
          content = createContent(panel, currentValueModel)
        }
        else {
          valueModel = null
          content = when {
            model.loading -> createLoadingLabelPanel()
            model.error != null -> createErrorPanel(model.error!!)
            else -> createEmptyContent()
          }
        }
      }

      abstract fun createContent(parentPanel: JPanel, valueModel: SingleValueModel<T>): JComponent

      private fun createEmptyContent(): JComponent? {
        if (notLoadingText == null) return null

        val pane = HtmlEditorPane(notLoadingText).apply {
          isFocusable = true
          foreground = UIUtil.getContextHelpForeground()
        }
        return JPanel(SingleComponentCenteringLayout()).apply {
          isOpaque = false
          add(pane)
        }
      }

      private fun createErrorPanel(error: Throwable): JComponent {
        return JPanel(SingleComponentCenteringLayout()).apply {
          isOpaque = false
          border = JBUI.Borders.empty(8)

          add(GEHtmlErrorPanel.create(errorPrefix, error, errorHandler?.getActionForError(error)))
        }
      }

      private fun createLoadingLabelPanel() = JPanel(SingleComponentCenteringLayout()).apply {
        isOpaque = false
        add(JLabel().apply {
          isFocusable = true
          foreground = UIUtil.getContextHelpForeground()
          text = ApplicationBundle.message("label.loading.page.please.wait")
          icon = AnimatedIcon.Default()
        })
      }
    }
  }
}