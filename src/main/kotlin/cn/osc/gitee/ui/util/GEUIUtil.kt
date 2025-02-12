// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.ui.util

import com.intellij.UtilBundle
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.collaboration.ui.CollaborationToolsUIUtil
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.editor.impl.view.FontLayoutService
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.*
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.*
import com.intellij.util.ui.components.BorderLayoutPanel
import icons.CollaborationToolsIcons
import cn.osc.gitee.GiteeIcons
import cn.osc.gitee.api.GERepositoryCoordinates
import cn.osc.gitee.api.data.GELabel
import cn.osc.gitee.api.data.GEUser
import cn.osc.gitee.api.data.GiteeIssueState
import cn.osc.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import cn.osc.gitee.api.data.pullrequest.GEPullRequestState
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.ui.avatars.GEAvatarIconsProvider
import com.intellij.collaboration.util.CollectionDelta
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.swing.*

object GEUIUtil {
  const val AVATAR_SIZE = 20

  fun getPullRequestStateIcon(state: GEPullRequestState, isDraft: Boolean): Icon =
    if (isDraft) GiteeIcons.PullRequestDraft
    else when (state) {
      GEPullRequestState.CLOSED -> CollaborationToolsIcons.PullRequestClosed
      GEPullRequestState.MERGED -> GiteeIcons.PullRequestMerged
      GEPullRequestState.OPEN -> CollaborationToolsIcons.PullRequestOpen
    }

  @NlsSafe
  fun getPullRequestStateText(state: GEPullRequestState, isDraft: Boolean): String =
    if (isDraft) GiteeBundle.message("pull.request.state.draft")
    else when (state) {
      GEPullRequestState.CLOSED -> GiteeBundle.message("pull.request.state.closed")
      GEPullRequestState.MERGED -> GiteeBundle.message("pull.request.state.merged")
      GEPullRequestState.OPEN -> GiteeBundle.message("pull.request.state.open")
    }

  fun getIssueStateIcon(state: GiteeIssueState): Icon =
    when (state) {
      GiteeIssueState.open -> GiteeIcons.IssueOpened
      GiteeIssueState.closed -> GiteeIcons.IssueClosed
    }

  @NlsSafe
  fun getIssueStateText(state: GiteeIssueState): String =
    when (state) {
      GiteeIssueState.open -> GiteeBundle.message("issue.state.open")
      GiteeIssueState.closed -> GiteeBundle.message("issue.state.closed")
    }

  fun createIssueLabelLabel(label: GELabel): JBLabel = JBLabel(" ${label.name} ", UIUtil.ComponentStyle.SMALL).apply {
    background = getLabelBackground(label)
    foreground = getLabelForeground(background)
  }.andOpaque()

  fun getLabelBackground(label: GELabel): JBColor {
    val apiColor = ColorUtil.fromHex(label.color)
    return JBColor(apiColor, ColorUtil.darker(apiColor, 3))
  }

  fun getLabelForeground(bg: Color): Color = if (ColorUtil.isDark(bg)) Color.white else Color.black

  private fun getFontEM(component: JComponent): Float {
    val metrics = component.getFontMetrics(component.font)
    //em dash character
    return FontLayoutService.getInstance().charWidth2D(metrics, '\u2014'.code)
  }

  fun formatActionDate(date: Date): String {
    val prettyDate = DateFormatUtil.formatPrettyDate(date).toLowerCase()
    val datePrefix = if (prettyDate.equals(UtilBundle.message("date.format.today"), true) ||
                         prettyDate.equals(UtilBundle.message("date.format.yesterday"), true)) ""
    else "on "
    return datePrefix + prettyDate
  }

  fun <T> showChooserPopup(@NlsContexts.PopupTitle popupTitle: String, parentComponent: JComponent,
                           cellRenderer: SelectionListCellRenderer<T>,
                           currentList: List<T>,
                           availableListFuture: CompletableFuture<List<T>>)
    : CompletableFuture<CollectionDelta<T>> {

    val listModel = CollectionListModel<SelectableWrapper<T>>()
    val list = JBList(listModel).apply {
      visibleRowCount = 7
      isFocusable = false
      selectionMode = ListSelectionModel.SINGLE_SELECTION
    }
    list.cellRenderer = cellRenderer

    val scrollPane = ScrollPaneFactory.createScrollPane(list, true).apply {
      viewport.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
      isFocusable = false
    }

    val searchField = SearchTextField(false).apply {
      border = IdeBorderFactory.createBorder(SideBorder.BOTTOM)
      UIUtil.setBackgroundRecursively(this, UIUtil.getListBackground())
      textEditor.border = JBUI.Borders.empty()
    }
    CollaborationToolsUIUtil.attachSearch(list, searchField) {
      cellRenderer.getText(it.value)
    }

    val panel = JBUI.Panels.simplePanel(scrollPane).addToTop(searchField.textEditor)
    ListUtil.installAutoSelectOnMouseMove(list)

    fun toggleSelection() {
      for (item in list.selectedValuesList) {
        item.selected = !item.selected
      }
      list.repaint()
    }

    list.addMouseListener(object : MouseAdapter() {
      override fun mouseReleased(e: MouseEvent) {
        if (UIUtil.isActionClick(e, MouseEvent.MOUSE_RELEASED) && !UIUtil.isSelectionButtonDown(e) && !e.isConsumed) toggleSelection()
      }
    })

    val originalSelection: Set<T> = currentList.toHashSet()
    listModel.add(currentList.map { SelectableWrapper(it, true) })

    val result = CompletableFuture<CollectionDelta<T>>()
    JBPopupFactory.getInstance().createComponentPopupBuilder(panel, searchField)
      .setRequestFocus(true)
      .setCancelOnClickOutside(true)
      .setTitle(popupTitle)
      .setResizable(true)
      .setMovable(true)
      .setKeyboardActions(listOf(Pair.create(ActionListener { toggleSelection() }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))))
      .addListener(object : JBPopupListener {
        override fun beforeShown(event: LightweightWindowEvent) {
          list.setPaintBusy(true)
          list.emptyText.text = ApplicationBundle.message("label.loading.page.please.wait")

          availableListFuture
            .thenApplyAsync { available ->
              available.map { SelectableWrapper(it, originalSelection.contains(it)) }
                .sortedWith(Comparator.comparing<SelectableWrapper<T>, Boolean> { !it.selected }
                              .thenComparing({ cellRenderer.getText(it.value) }) { a, b -> StringUtil.compare(a, b, true) })
            }.successOnEdt {
              listModel.replaceAll(it)

              list.setPaintBusy(false)
              list.emptyText.text = UIBundle.message("message.noMatchesFound")

              event.asPopup().pack(true, true)

              if (list.selectedIndex == -1) {
                list.selectedIndex = 0
              }
            }
        }

        override fun onClosed(event: LightweightWindowEvent) {
          val selected = listModel.items.filter { it.selected }.map { it.value }
          result.complete(CollectionDelta(originalSelection, selected))
        }
      })
      .createPopup()
      .showUnderneathOf(parentComponent)
    return result
  }

  fun getPRTimelineWidth() = (getFontEM(JLabel()) * 42).toInt()

  data class SelectableWrapper<T>(val value: T, var selected: Boolean = false)

  sealed class SelectionListCellRenderer<T> : ListCellRenderer<SelectableWrapper<T>>, BorderLayoutPanel() {

    private val mainLabel = JLabel()
    private val checkIconLabel = JLabel()

    init {
      checkIconLabel.iconTextGap = JBUI.scale(UIUtil.DEFAULT_VGAP)
      checkIconLabel.border = JBUI.Borders.empty(0, 4)

      addToLeft(checkIconLabel)
      addToCenter(mainLabel)

      border = JBUI.Borders.empty(4, 0)
    }

    override fun getListCellRendererComponent(list: JList<out SelectableWrapper<T>>,
                                              value: SelectableWrapper<T>,
                                              index: Int,
                                              isSelected: Boolean,
                                              cellHasFocus: Boolean): Component {
      foreground = UIUtil.getListForeground(isSelected, true)
      background = UIUtil.getListBackground(isSelected, true)

      mainLabel.foreground = foreground
      mainLabel.font = font

      mainLabel.text = getText(value.value)
      mainLabel.icon = getIcon(value.value)

      val icon = LafIconLookup.getIcon("checkmark", isSelected, false)
      checkIconLabel.icon = if (value.selected) icon else EmptyIcon.create(icon)

      return this
    }

    @NlsContexts.Label
    abstract fun getText(value: T): String
    abstract fun getIcon(value: T): Icon

    class PRReviewers(private val iconsProvider: GEAvatarIconsProvider)
      : SelectionListCellRenderer<GEPullRequestRequestedReviewer>() {
      override fun getText(value: GEPullRequestRequestedReviewer) = value.shortName
      override fun getIcon(value: GEPullRequestRequestedReviewer) = iconsProvider.getIcon(value.avatarUrl, AVATAR_SIZE)
    }

    class Users(private val iconsProvider: GEAvatarIconsProvider)
      : SelectionListCellRenderer<GEUser>() {
      override fun getText(value: GEUser) = value.login
      override fun getIcon(value: GEUser) = iconsProvider.getIcon(value.avatarUrl, AVATAR_SIZE)
    }

    class Labels : SelectionListCellRenderer<GELabel>() {
      override fun getText(value: GELabel) = value.name
      override fun getIcon(value: GELabel) = ColorIcon(16, ColorUtil.fromHex(value.color))
    }
  }

  @NlsSafe
  fun getRepositoryDisplayName(allRepositories: List<GERepositoryCoordinates>,
                               repository: GERepositoryCoordinates,
                               alwaysShowOwner: Boolean = false): String {
    val showServer = needToShowRepositoryServer(allRepositories)
    val showOwner = if (showServer || alwaysShowOwner) true else needToShowRepositoryOwner(allRepositories)

    val builder = StringBuilder()
    if (showServer) builder.append(repository.serverPath.toUrl(false)).append("/")
    if (showOwner) builder.append(repository.repositoryPath.owner).append("/")
    builder.append(repository.repositoryPath.repository)
    return builder.toString()
  }

  /**
   * Assuming all servers are the same
   */
  private fun needToShowRepositoryOwner(repos: List<GERepositoryCoordinates>): Boolean {
    if (repos.size <= 1) return false
    val firstOwner = repos.first().repositoryPath.owner
    return repos.any { it.repositoryPath.owner != firstOwner }
  }

  private fun needToShowRepositoryServer(repos: List<GERepositoryCoordinates>): Boolean {
    if (repos.size <= 1) return false
    val firstServer = repos.first().serverPath
    return repos.any { it.serverPath != firstServer }
  }
}

@NlsSafe
fun Action.getName(): String = (getValue(Action.NAME) as? String).orEmpty()