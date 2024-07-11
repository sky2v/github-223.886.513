// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data

import cn.osc.gitee.api.data.pullrequest.timeline.GEPRTimelineItem
import java.util.*

open class GEIssueComment(id: String,
                          author: GEActor?,
                          body: String,
                          createdAt: Date,
                          val viewerCanDelete: Boolean,
                          val viewerCanUpdate: Boolean)
  : GEComment(id, author, body, createdAt), GEPRTimelineItem