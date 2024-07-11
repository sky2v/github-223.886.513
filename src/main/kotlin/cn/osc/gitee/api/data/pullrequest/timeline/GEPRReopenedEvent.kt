// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data.pullrequest.timeline

import cn.osc.gitee.api.data.GEActor
import cn.osc.gitee.api.data.pullrequest.GEPullRequestState
import java.util.*

class GEPRReopenedEvent(override val actor: GEActor?,
                        override val createdAt: Date)
  : GEPRTimelineEvent.State {
  override val newState = GEPullRequestState.OPEN
}