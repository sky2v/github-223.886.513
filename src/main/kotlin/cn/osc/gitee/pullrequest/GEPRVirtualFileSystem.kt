// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.vcs.editor.ComplexPathVirtualFileSystem
import com.intellij.vcs.editor.GsonComplexPathSerializer
import cn.osc.gitee.api.GERepositoryCoordinates
import cn.osc.gitee.pullrequest.data.GEPRDataContextRepository
import cn.osc.gitee.pullrequest.data.GEPRIdentifier
import cn.osc.gitee.pullrequest.data.SimpleGHPRIdentifier

internal class GEPRVirtualFileSystem : ComplexPathVirtualFileSystem<GEPRVirtualFileSystem.GEPRFilePath>(
  GsonComplexPathSerializer(GEPRFilePath::class.java)
) {
  override fun getProtocol() = PROTOCOL

  override fun findOrCreateFile(project: Project, path: GEPRFilePath): VirtualFile? {
    val filesManager = GEPRDataContextRepository.getInstance(project).findContext(path.repository)?.filesManager ?: return null
    val pullRequest = path.prId
    val sourceId = path.sourceId
    return when {
      pullRequest == null && sourceId != null -> filesManager.createOrGetNewPRDiffFile(sourceId, true)
      pullRequest == null -> null
      path.isDiff -> filesManager.findDiffFile(pullRequest)
      else -> filesManager.findTimelineFile(pullRequest)
    }
  }

  fun getPath(fileManagerId: String,
              project: Project,
              repository: GERepositoryCoordinates,
              id: GEPRIdentifier?,
              sourceId: String?,
              isDiff: Boolean = false): String =
    getPath(GEPRFilePath(fileManagerId, project.locationHash, repository, id?.let { SimpleGHPRIdentifier(it) }, sourceId, isDiff))

  data class GEPRFilePath(override val sessionId: String,
                          override val projectHash: String,
                          val repository: GERepositoryCoordinates,
                          val prId: SimpleGHPRIdentifier?,
                          val sourceId: String?,
                          val isDiff: Boolean) : ComplexPath

  companion object {
    private const val PROTOCOL = "ghpr"

    fun getInstance() = service<VirtualFileManager>().getFileSystem(PROTOCOL) as GEPRVirtualFileSystem
  }
}
