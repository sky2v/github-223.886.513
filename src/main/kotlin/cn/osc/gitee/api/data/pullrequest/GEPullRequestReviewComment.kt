// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data.pullrequest

import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.collaboration.api.dto.GraphQLFragment
import cn.osc.gitee.api.data.GEActor
import cn.osc.gitee.api.data.GEComment
import cn.osc.gitee.api.data.GECommitHash
import cn.osc.gitee.api.data.GENode
import java.util.*

@GraphQLFragment("/graphql/fragment/pullRequestReviewComment.graphql")
open class GEPullRequestReviewComment(id: String,
                                      val databaseId: Long,
                                      val url: String,
                                      author: GEActor?,
                                      body: String,
                                      createdAt: Date,
                                      val state: GEPullRequestReviewCommentState,
                                      val commit: GECommitHash?,
                                      val originalCommit: GECommitHash?,
                                      val replyTo: GENode?,
                                      val diffHunk: String,
                                      @JsonProperty("pullRequestReview") pullRequestReview: GENode?,
                                      val viewerCanDelete: Boolean,
                                      val viewerCanUpdate: Boolean)
  : GEComment(id, author, body, createdAt) {
  val reviewId = pullRequestReview?.id
}