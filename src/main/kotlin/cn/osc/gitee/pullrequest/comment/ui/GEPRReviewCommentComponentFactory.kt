// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.pullrequest.comment.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.panels.VerticalLayout
import org.jetbrains.annotations.VisibleForTesting
import cn.osc.gitee.pullrequest.comment.GEMarkdownToHtmlConverter
import cn.osc.gitee.pullrequest.comment.GESuggestedChange
import cn.osc.gitee.pullrequest.comment.GESuggestedChangeApplier
import cn.osc.gitee.pullrequest.ui.changes.GEPRSuggestedChangeHelper
import cn.osc.gitee.ui.util.HtmlEditorPane
import javax.swing.JComponent
import javax.swing.JPanel


class GEPRReviewCommentComponentFactory(private val project: Project) {
  private val markdownConverter = GEMarkdownToHtmlConverter(project)

  fun createCommentComponent(commentBody: String): JComponent {
    val htmlBody = markdownConverter.convertMarkdown(commentBody)
    return HtmlEditorPane(htmlBody)
  }

  fun createCommentWithSuggestedChangeComponent(
    thread: GEPRReviewThreadModel,
    suggestedChange: GESuggestedChange,
    suggestedChangeHelper: GEPRSuggestedChangeHelper
  ): JComponent {
    val htmlBody = markdownConverter.convertMarkdownWithSuggestedChange(suggestedChange)
    val content = htmlBody.removePrefix("<body>").removeSuffix("</body>")
    val commentBlocks = collectCommentBlocks(content)

    val suggestedChangeApplier = GESuggestedChangeApplier(project, suggestedChangeHelper.repository, suggestedChange)
    val suggestedChangeComponent = GEPRReviewSuggestedChangeComponentFactory(project, thread, suggestedChangeApplier, suggestedChangeHelper)

    return JPanel(VerticalLayout(0)).apply {
      isOpaque = false

      commentBlocks.forEach {
        when (it.commentType) {
          CommentType.COMMENT -> add(HtmlEditorPane(it.content))
          CommentType.SUGGESTED_CHANGE -> add(suggestedChangeComponent.create(it.content))
        }
      }
    }
  }

  @VisibleForTesting
  enum class CommentType {
    COMMENT,
    SUGGESTED_CHANGE
  }

  @VisibleForTesting
  data class CommentBlock(
    val commentType: CommentType,
    val content: String
  )

  companion object {
    private const val SUGGESTED_CHANGE_START_TAG = "<code class=\"language-suggestion\">"
    private const val SUGGESTED_CHANGE_END_TAG = "</code>"

    @VisibleForTesting
    fun collectCommentBlocks(comment: String): List<CommentBlock> {
      val commentBlockRanges = collectCommentBlockRanges(comment)

      var isSuggestedChange = false
      val result = mutableListOf<CommentBlock>()
      for (range in commentBlockRanges) {
        if (range.last > range.first) {
          val commentBlock =
            if (isSuggestedChange) CommentBlock(CommentType.SUGGESTED_CHANGE, comment.substring(range.first, range.last))
            else CommentBlock(CommentType.COMMENT, comment.substring(range.first, range.last))
          result.add(commentBlock)
        }

        isSuggestedChange = !isSuggestedChange
      }

      return result
    }

    private fun collectCommentBlockRanges(comment: String): List<IntRange> {
      val indexes = mutableListOf(0)

      var startIndex = comment.indexOf(SUGGESTED_CHANGE_START_TAG)
      var endIndex = comment.indexOf(SUGGESTED_CHANGE_END_TAG) + SUGGESTED_CHANGE_END_TAG.length
      while (startIndex >= 0) {
        indexes.add(startIndex)
        indexes.add(endIndex)

        startIndex = comment.indexOf(SUGGESTED_CHANGE_START_TAG, startIndex + 1)
        endIndex = comment.indexOf(SUGGESTED_CHANGE_END_TAG, endIndex + 1) + SUGGESTED_CHANGE_END_TAG.length
      }
      indexes.add(comment.length)

      return indexes.zipWithNext() { start, end -> start..end }
    }
  }
}