// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data.pullrequest

import com.intellij.collaboration.api.dto.GraphQLFragment
import cn.osc.gitee.api.data.GENode

@GraphQLFragment("/graphql/fragment/teamInfo.graphql")
class GETeam(id: String,
             val slug: String,
             override val url: String,
             override val avatarUrl: String,
             override val name: String?,
             val combinedSlug: String)
  : GENode(id), GEPullRequestRequestedReviewer {
  override val shortName: String = combinedSlug
}