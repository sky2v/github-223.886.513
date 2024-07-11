// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.data

import com.intellij.openapi.vcs.changes.Change
import cn.osc.gitee.github.api.data.GHCommit

interface GHPRChangesProvider {
  val changes: List<Change>
  val changesByCommits: Map<String, List<Change>>
  val linearHistory: Boolean

  fun findChangeDiffData(change: Change): GHPRChangeDiffData?

  fun findCumulativeChange(commitSha: String, filePath: String): Change?
}