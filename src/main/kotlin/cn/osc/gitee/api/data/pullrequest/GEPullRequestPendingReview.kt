// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data.pullrequest

import com.intellij.collaboration.api.dto.GraphQLFragment
import cn.osc.gitee.api.data.GENode
import cn.osc.gitee.api.data.GENodes

@GraphQLFragment("/graphql/fragment/pullRequestPendingReview.graphql")
open class GEPullRequestPendingReview(id: String,
                                      val state: GEPullRequestReviewState,
                                      val comments: GENodes<GEPullRequestReviewComment>)
  : GENode(id)