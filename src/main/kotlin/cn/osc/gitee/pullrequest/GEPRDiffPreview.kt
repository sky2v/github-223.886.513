// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.pullrequest

import com.intellij.diff.chains.DiffRequestProducer
import com.intellij.diff.util.DiffUserDataKeys
import com.intellij.diff.util.DiffUtil
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.NonEmptyActionGroup
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeViewDiffRequestProcessor.ChangeWrapper
import com.intellij.openapi.vcs.changes.ChangeViewDiffRequestProcessor.Wrapper
import com.intellij.openapi.vcs.changes.DiffPreview
import com.intellij.openapi.vcs.changes.DiffPreviewController
import com.intellij.openapi.vcs.changes.DiffPreviewControllerBase
import com.intellij.openapi.vcs.changes.actions.diff.CombinedDiffPreview
import com.intellij.openapi.vcs.changes.actions.diff.CombinedDiffPreviewModel
import com.intellij.openapi.vcs.changes.ui.ChangesTree
import com.intellij.openapi.vcs.changes.ui.VcsTreeModelData
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.Processor
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.pullrequest.action.GEPRActionKeys
import cn.osc.gitee.pullrequest.action.GEPRShowDiffActionProvider
import cn.osc.gitee.pullrequest.comment.action.combined.GEPRCombinedDiffReviewResolvedThreadsToggleAction
import cn.osc.gitee.pullrequest.comment.action.combined.GEPRCombinedDiffReviewThreadsReloadAction
import cn.osc.gitee.pullrequest.comment.action.combined.GEPRCombinedDiffReviewThreadsToggleAction
import cn.osc.gitee.pullrequest.data.GEPRFilesManager
import cn.osc.gitee.pullrequest.data.GEPRIdentifier
import cn.osc.gitee.pullrequest.data.provider.GEPRDataProvider
import cn.osc.gitee.util.ChangeDiffRequestProducerFactory
import cn.osc.gitee.util.GEToolbarLabelAction
import java.util.*

internal class GEPRDiffPreview(private val prId: GEPRIdentifier?,
                               private val filesManager: GEPRFilesManager) : DiffPreview {
  val sourceId = UUID.randomUUID().toString()

  override fun updateDiffAction(event: AnActionEvent) {
    GEPRShowDiffActionProvider.updateAvailability(event)
  }

  override fun openPreview(requestFocus: Boolean): Boolean {
    if (prId == null) {
      filesManager.createAndOpenNewPRDiffPreviewFile(sourceId, false, requestFocus)
    }
    else {
      filesManager.createAndOpenDiffFile(prId, requestFocus)
    }
    return true
  }

  override fun closePreview() {
  }
}

internal class GEPRCombinedDiffPreview(private val dataProvider: GEPRDataProvider?,
                                       private val filesManager: GEPRFilesManager,
                                       producerFactory: ChangeDiffRequestProducerFactory,
                                       tree: ChangesTree) :
  GEPRCombinedDiffPreviewBase(dataProvider, filesManager, producerFactory, tree, filesManager) {

  override fun doOpenPreview(requestFocus: Boolean): Boolean {
    val sourceId = tree.id
    val prId = dataProvider?.id
    if (prId == null) {
      filesManager.createAndOpenNewPRDiffPreviewFile(sourceId, true, requestFocus)
    }
    else {
      filesManager.createAndOpenDiffPreviewFile(prId, sourceId, requestFocus)
    }
    return true
  }
}

internal abstract class GEPRCombinedDiffPreviewBase(private val dataProvider: GEPRDataProvider?,
                                                    private val filesManager: GEPRFilesManager,
                                                    private val producerFactory: ChangeDiffRequestProducerFactory,
                                                    tree: ChangesTree,
                                                    parentDisposable: Disposable) : CombinedDiffPreview(tree, parentDisposable) {

  override val previewFile: VirtualFile
    get() =
      dataProvider?.id.let { prId ->
        if (prId == null) {
          filesManager.createOrGetNewPRDiffFile(tree.id, true)
        }
        else {
          filesManager.createOrGetDiffFile(prId, tree.id, true)
        }
      }

  override fun createModel(): CombinedDiffPreviewModel =
    GEPRCombinedDiffPreviewModel(tree, producerFactory, parentDisposable).also(::setupReviewActions)

  private fun setupReviewActions(model: CombinedDiffPreviewModel) {
    val context = model.context
    DiffUtil.putDataKey(context, GEPRActionKeys.PULL_REQUEST_DATA_PROVIDER, dataProvider)

    val viewOptionsGroup = NonEmptyActionGroup().apply {
      isPopup = true
      templatePresentation.text = GiteeBundle.message("pull.request.diff.view.options")
      templatePresentation.icon = AllIcons.Actions.Show
      add(GEPRCombinedDiffReviewThreadsToggleAction(model))
      add(GEPRCombinedDiffReviewResolvedThreadsToggleAction(model))
    }

    context.putUserData(DiffUserDataKeys.CONTEXT_ACTIONS, listOf(
      GEToolbarLabelAction(GiteeBundle.message("pull.request.diff.review.label")),
      viewOptionsGroup,
      GEPRCombinedDiffReviewThreadsReloadAction(model),
      ActionManager.getInstance().getAction("Gitee.PullRequest.Review.Submit")))
  }

  override fun getCombinedDiffTabTitle(): String = previewFile.name

  override fun updateDiffAction(event: AnActionEvent) {
    super.updateDiffAction(event)
    GEPRShowDiffActionProvider.updateAvailability(event)
  }

  override fun openPreview(requestFocus: Boolean): Boolean {
    if (!ensureHasContent()) return false
    return openPreviewEditor(requestFocus)
  }

  private fun ensureHasContent(): Boolean {
    updatePreviewProcessor.refresh(false)
    return hasContent()
  }

  private fun openPreviewEditor(requestFocus: Boolean): Boolean {
    escapeHandler?.let { handler -> registerEscapeHandler(previewFile, handler) }
    return doOpenPreview(requestFocus)
  }

  abstract fun doOpenPreview(requestFocus: Boolean): Boolean

  companion object {
    fun createAndSetupDiffPreview(tree: ChangesTree,
                                  producerFactory: ChangeDiffRequestProducerFactory,
                                  dataProvider: GEPRDataProvider?,
                                  filesManager: GEPRFilesManager): DiffPreviewController {
      val diffPreviewHolder =
        object : DiffPreviewControllerBase() {
          override val simplePreview = GEPRDiffPreview(dataProvider?.id, filesManager)
          override fun createCombinedDiffPreview() = GEPRCombinedDiffPreview(dataProvider, filesManager, producerFactory, tree)
        }

      tree.apply {
        doubleClickHandler = Processor { e ->
          if (EditSourceOnDoubleClickHandler.isToggleEvent(this, e)) return@Processor false
          diffPreviewHolder.activePreview.performDiffAction()
          true
        }
        enterKeyHandler = Processor {
          diffPreviewHolder.activePreview.performDiffAction()
          true
        }
      }

      return diffPreviewHolder
    }
  }
}

private class GEPRCombinedDiffPreviewModel(tree: ChangesTree,
                                           private val producerFactory: ChangeDiffRequestProducerFactory,
                                           parentDisposable: Disposable) :
  CombinedDiffPreviewModel(tree, prepareCombinedDiffModelRequests(tree.project, tree.getAllChanges(producerFactory)), parentDisposable) {

  override fun iterateAllChanges(): Iterable<Wrapper> {
    return tree.iterateAllChanges(producerFactory)
  }

  override fun iterateSelectedChanges(): Iterable<Wrapper> {
    return VcsTreeModelData.selected(tree).iterateUserObjects(Change::class.java)
      .map { change -> MyChangeWrapper(change, producerFactory) }
  }

  private class MyChangeWrapper(change: Change,
                                private val changeProducerFactory: ChangeDiffRequestProducerFactory) : ChangeWrapper(change) {
    override fun createProducer(project: Project?): DiffRequestProducer? = changeProducerFactory.create(project, change)
  }

  companion object {
    private fun ChangesTree.getAllChanges(producerFactory: ChangeDiffRequestProducerFactory) = iterateAllChanges(producerFactory).toList()

    private fun ChangesTree.iterateAllChanges(producerFactory: ChangeDiffRequestProducerFactory): Iterable<Wrapper> {
      return VcsTreeModelData.all(this).iterateUserObjects(Change::class.java)
        .map { change -> MyChangeWrapper(change, producerFactory) }
    }
  }
}
