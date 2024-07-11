// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data

import com.intellij.diff.editor.DiffEditorTabFilesManager
import com.intellij.diff.editor.DiffVirtualFileBase
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.util.EventDispatcher
import com.intellij.util.containers.ContainerUtil
import cn.osc.gitee.api.GERepositoryCoordinates
import cn.osc.gitee.api.data.pullrequest.GEPullRequestShort
import cn.osc.gitee.pullrequest.*
import java.util.*

internal class GEPRFilesManagerImpl(private val project: Project,
                                    private val repository: GERepositoryCoordinates) : GEPRFilesManager {

  // current time should be enough to distinguish the manager between launches
  private val id = System.currentTimeMillis().toString()

  private val filesEventDispatcher = EventDispatcher.create(FileListener::class.java)

  private val files = ContainerUtil.createWeakValueMap<GEPRIdentifier, GEPRTimelineVirtualFile>()
  private val diffFiles = ContainerUtil.createWeakValueMap<GEPRIdentifier, DiffVirtualFileBase>()
  private var newPRDiffFiles = ContainerUtil.createWeakValueMap<String, DiffVirtualFileBase>()

  override fun createOrGetNewPRDiffFile(sourceId: String, combinedDiff: Boolean): DiffVirtualFileBase {
    return newPRDiffFiles.getOrPut(sourceId) {
      if (combinedDiff) {
        GENewPRCombinedDiffPreviewVirtualFile(sourceId, id, project, repository)
      }
      else {
        GENewPRDiffVirtualFile(id, project, repository)
      }
    }
  }

  override fun createOrGetDiffFile(pullRequest: GEPRIdentifier, sourceId: String, combinedDiff: Boolean): DiffVirtualFileBase {
    return diffFiles.getOrPut(SimpleGHPRIdentifier(pullRequest)) {
      if (combinedDiff) {
        GEPRCombinedDiffPreviewVirtualFile(sourceId, id, project, repository, pullRequest)
      }
      else {
        GEPRDiffVirtualFile(id, project, repository, pullRequest)
      }
    }
  }

  override fun createAndOpenTimelineFile(pullRequest: GEPRIdentifier, requestFocus: Boolean) {
    files.getOrPut(SimpleGHPRIdentifier(pullRequest)) {
      GEPRTimelineVirtualFile(id, project, repository, pullRequest)
    }.let {
      filesEventDispatcher.multicaster.onBeforeFileOpened(it)
      FileEditorManager.getInstance(project).openFile(it, requestFocus)
      GEPRStatisticsCollector.logTimelineOpened(project)
    }
  }

  override fun createAndOpenDiffFile(pullRequest: GEPRIdentifier, requestFocus: Boolean) {
    diffFiles.getOrPut(SimpleGHPRIdentifier(pullRequest)) {
      GEPRDiffVirtualFile(id, project, repository, pullRequest)
    }.let {
      DiffEditorTabFilesManager.getInstance(project).showDiffFile(it, requestFocus)
      GEPRStatisticsCollector.logDiffOpened(project)
    }
  }

  override fun createAndOpenDiffPreviewFile(pullRequest: GEPRIdentifier, sourceId: String, requestFocus: Boolean): DiffVirtualFileBase {
    return diffFiles.getOrPut(SimpleGHPRIdentifier(pullRequest)) {
      GEPRCombinedDiffPreviewVirtualFile(sourceId, id, project, repository, pullRequest)
    }.also {
      DiffEditorTabFilesManager.getInstance(project).showDiffFile(it, requestFocus)
      GEPRStatisticsCollector.logDiffOpened(project)
    }
  }

  override fun createAndOpenNewPRDiffPreviewFile(sourceId: String, combinedDiff: Boolean, requestFocus: Boolean): DiffVirtualFileBase {
    return newPRDiffFiles.getOrPut(sourceId) {
      if (combinedDiff) {
        GENewPRCombinedDiffPreviewVirtualFile(sourceId, id, project, repository)
      }
      else {
        GENewPRDiffVirtualFile(id, project, repository)
      }
    }.also {
      DiffEditorTabFilesManager.getInstance(project).showDiffFile(it, requestFocus)
      GEPRStatisticsCollector.logDiffOpened(project)
    }
  }

  override fun findTimelineFile(pullRequest: GEPRIdentifier): GEPRTimelineVirtualFile? = files[SimpleGHPRIdentifier(pullRequest)]

  override fun findDiffFile(pullRequest: GEPRIdentifier): DiffVirtualFileBase? = diffFiles[SimpleGHPRIdentifier(pullRequest)]

  override fun updateTimelineFilePresentation(details: GEPullRequestShort) {
    val file = findTimelineFile(details)
    if (file != null) {
      file.details = details
      FileEditorManagerEx.getInstanceEx(project).updateFilePresentation(file)
    }
  }

  override fun addBeforeTimelineFileOpenedListener(disposable: Disposable, listener: (file: GEPRTimelineVirtualFile) -> Unit) {
    filesEventDispatcher.addListener(object : FileListener {
      override fun onBeforeFileOpened(file: GEPRTimelineVirtualFile) = listener(file)
    }, disposable)
  }

  override fun dispose() {
    for (file in (files.values + diffFiles.values + newPRDiffFiles.values)) {
      FileEditorManager.getInstance(project).closeFile(file)
      file.isValid = false
    }
  }

  private interface FileListener : EventListener {
    fun onBeforeFileOpened(file: GEPRTimelineVirtualFile)
  }
}
