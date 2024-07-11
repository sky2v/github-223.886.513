// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.api.data.pullrequest

import com.intellij.collaboration.api.dto.GraphQLFragment
import cn.osc.gitee.github.api.data.GHActor
import cn.osc.gitee.github.api.data.GHNode
import cn.osc.gitee.github.api.data.pullrequest.timeline.GHPRTimelineItem
import java.util.*

@GraphQLFragment("/graphql/fragment/pullRequestReview.graphql")
open class GHPullRequestReview(id: String,
                               val url: String,
                               val author: GHActor?,
                               val body: String,
                               val state: GHPullRequestReviewState,
                               val createdAt: Date,
                               val viewerCanUpdate: Boolean)
  : GHNode(id), GHPRTimelineItem