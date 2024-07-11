// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.api.data.pullrequest.timeline

import cn.osc.gitee.github.api.data.GHActor
import cn.osc.gitee.github.api.data.GHCommitHash
import cn.osc.gitee.github.api.data.pullrequest.GHGitRefName
import java.util.*

class GHPRHeadRefForcePushedEvent(override val actor: GHActor?,
                                  override val createdAt: Date,
                                  val ref: GHGitRefName?,
                                  val beforeCommit: GHCommitHash,
                                  val afterCommit: GHCommitHash)
  : GHPRTimelineEvent.Branch
