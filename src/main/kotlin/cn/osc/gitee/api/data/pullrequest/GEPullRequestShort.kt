// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data.pullrequest

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.collaboration.api.dto.GraphQLFragment
import com.intellij.openapi.util.NlsSafe
import cn.osc.gitee.api.data.*
import cn.osc.gitee.pullrequest.data.GEPRIdentifier
import java.util.*

@GraphQLFragment("/graphql/fragment/pullRequestInfoShort.graphql")
open class GEPullRequestShort(id: String,
                              val url: String,
                              override val number: Long,
                              @NlsSafe val title: String,
                              val state: GEPullRequestState,
                              val isDraft: Boolean,
                              val author: GEActor?,
                              val createdAt: Date,
                              @JsonProperty("assignees") assignees: GENodes<GEUser>,
                              @JsonProperty("labels") labels: GENodes<GELabel>,
                              @JsonProperty("reviewRequests") reviewRequests: GENodes<GEPullRequestReviewRequest>,
                              @JsonProperty("reviewThreads") reviewThreads: GENodes<ReviewThreadDetails>,
                              val mergeable: GEPullRequestMergeableState,
                              val viewerCanUpdate: Boolean,
                              val viewerDidAuthor: Boolean) : GENode(id), GEPRIdentifier {

  @JsonIgnore
  val assignees = assignees.nodes

  @JsonIgnore
  val labels = labels.nodes

  @JsonIgnore
  val reviewRequests = reviewRequests.nodes

  @JsonIgnore
  val unresolvedReviewThreadsCount = reviewThreads.nodes.count { !it.isResolved && !it.isOutdated }

  override fun toString(): String = "#$number $title"

  class ReviewThreadDetails(val isResolved: Boolean, val isOutdated: Boolean)
}