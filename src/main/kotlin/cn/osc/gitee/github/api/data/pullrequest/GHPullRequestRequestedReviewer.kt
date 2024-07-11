// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.api.data.pullrequest

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.intellij.collaboration.api.dto.GraphQLFragment
import cn.osc.gitee.github.api.data.GHUser

@GraphQLFragment("/graphql/fragment/pullRequestReviewerInfo.graphql")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "__typename", visible = false)
@JsonSubTypes(
  JsonSubTypes.Type(name = "User", value = GHUser::class),
  JsonSubTypes.Type(name = "Team", value = GHTeam::class)
)
interface GHPullRequestRequestedReviewer {
  val shortName: String
  val url: String
  val avatarUrl: String
  val name: String?
}