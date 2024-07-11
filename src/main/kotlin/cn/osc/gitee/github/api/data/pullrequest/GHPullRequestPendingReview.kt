// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.api.data.pullrequest

import com.intellij.collaboration.api.dto.GraphQLFragment
import cn.osc.gitee.github.api.data.GHNode
import cn.osc.gitee.github.api.data.GHNodes

@GraphQLFragment("/graphql/fragment/pullRequestPendingReview.graphql")
open class GHPullRequestPendingReview(id: String,
                                      val state: GHPullRequestReviewState,
                                      val comments: GHNodes<GHPullRequestReviewComment>)
  : GHNode(id)