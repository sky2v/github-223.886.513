// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data.pullrequest.timeline

import cn.osc.gitee.api.data.GEActor
import java.util.*

class GEPRHeadRefDeletedEvent(override val actor: GEActor?,
                              override val createdAt: Date,
                              val headRefName: String)
  : GEPRTimelineEvent.Branch
