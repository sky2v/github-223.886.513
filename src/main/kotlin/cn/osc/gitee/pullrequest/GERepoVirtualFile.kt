// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest

import com.intellij.diff.editor.DiffVirtualFileBase.Companion.turnOffReopeningWindow
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFilePathWrapper
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.VirtualFileWithoutContent
import com.intellij.testFramework.LightVirtualFileBase
import cn.osc.gitee.api.GERepositoryCoordinates

/**
 * [fileManagerId] is a [cn.osc.gitee.pullrequest.data.GEPRFilesManagerImpl.id] which is required to differentiate files
 * between launches of a PR toolwindow.
 * This is necessary to make the files appear in "Recent Files" correctly.
 * See [com.intellij.vcs.editor.ComplexPathVirtualFileSystem.ComplexPath.sessionId] for details.
 */
abstract class GERepoVirtualFile(protected val fileManagerId: String,
                                 val project: Project,
                                 val repository: GERepositoryCoordinates)
  : LightVirtualFileBase("", null, 0), VirtualFileWithoutContent, VirtualFilePathWrapper {

  init {
    turnOffReopeningWindow()
  }

  override fun enforcePresentableName() = true

  override fun getFileSystem(): VirtualFileSystem = GEPRVirtualFileSystem.getInstance()
  override fun getFileType(): FileType = FileTypes.UNKNOWN

  override fun getLength() = 0L
  override fun contentsToByteArray() = throw UnsupportedOperationException()
  override fun getInputStream() = throw UnsupportedOperationException()
  override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long) = throw UnsupportedOperationException()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GERepoVirtualFile) return false

    if (fileManagerId != other.fileManagerId) return false
    if (project != other.project) return false
    if (repository != other.repository) return false

    return true
  }

  override fun hashCode(): Int {
    var result = fileManagerId.hashCode()
    result = 31 * result + project.hashCode()
    result = 31 * result + repository.hashCode()
    return result
  }
}
