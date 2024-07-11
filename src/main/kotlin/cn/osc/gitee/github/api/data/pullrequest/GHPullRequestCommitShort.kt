// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.api.data.pullrequest

import cn.osc.gitee.github.api.data.GHCommitShort
import cn.osc.gitee.github.api.data.GHNode
import cn.osc.gitee.github.api.data.pullrequest.timeline.GHPRTimelineItem

class GHPullRequestCommitShort(id: String, val commit: GHCommitShort, val url: String) : GHNode(id), GHPRTimelineItem