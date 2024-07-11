// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data

import com.intellij.openapi.diff.impl.patch.FilePatch
import cn.osc.gitee.api.data.GECommit

class GECommitWithPatches(val commit: GECommit,
                          val commitPatches: List<FilePatch>,
                          val cumulativePatches: List<FilePatch>) {

  val sha = commit.oid
  val parents = commit.parents.map { it.oid }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GECommitWithPatches) return false

    if (commit != other.commit) return false

    return true
  }

  override fun hashCode(): Int {
    return commit.hashCode()
  }
}