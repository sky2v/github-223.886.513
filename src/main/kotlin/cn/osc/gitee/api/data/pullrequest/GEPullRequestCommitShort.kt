// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data.pullrequest

import cn.osc.gitee.api.data.GECommitShort
import cn.osc.gitee.api.data.GENode
import cn.osc.gitee.api.data.pullrequest.timeline.GEPRTimelineItem

class GEPullRequestCommitShort(id: String, val commit: GECommitShort, val url: String) : GENode(id), GEPRTimelineItem