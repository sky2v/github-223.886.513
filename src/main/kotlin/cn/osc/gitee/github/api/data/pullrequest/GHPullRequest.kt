// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.api.data.pullrequest

import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.collaboration.api.dto.GraphQLFragment
import com.intellij.openapi.util.NlsSafe
import cn.osc.gitee.github.api.data.GHActor
import cn.osc.gitee.github.api.data.GHLabel
import cn.osc.gitee.github.api.data.GHNodes
import cn.osc.gitee.github.api.data.GHUser
import java.util.*

@GraphQLFragment("/graphql/fragment/pullRequestInfo.graphql")
class GHPullRequest(id: String,
                    url: String,
                    number: Long,
                    title: String,
                    state: GHPullRequestState,
                    isDraft: Boolean,
                    author: GHActor?,
                    createdAt: Date,
                    @JsonProperty("assignees") assignees: GHNodes<GHUser>,
                    @JsonProperty("labels") labels: GHNodes<GHLabel>,
                    @JsonProperty("reviewRequests") reviewRequests: GHNodes<GHPullRequestReviewRequest>,
                    @JsonProperty("reviewThreads") reviewThreads: GHNodes<ReviewThreadDetails>,
                    mergeable: GHPullRequestMergeableState,
                    viewerCanUpdate: Boolean,
                    viewerDidAuthor: Boolean,
                    @NlsSafe val body: String,
                    val baseRefName: String,
                    val baseRefOid: String,
                    val baseRepository: Repository?,
                    val headRefName: String,
                    val headRefOid: String,
                    val headRepository: HeadRepository?)
  : GHPullRequestShort(id, url, number, title, state, isDraft, author, createdAt, assignees, labels, reviewRequests, reviewThreads,
                       mergeable, viewerCanUpdate, viewerDidAuthor) {

  open class Repository(val owner: Owner, val isFork: Boolean)

  class HeadRepository(owner: Owner, isFork: Boolean, val url: String, val sshUrl: String) : Repository(owner, isFork)

  class Owner(val login: String)
}