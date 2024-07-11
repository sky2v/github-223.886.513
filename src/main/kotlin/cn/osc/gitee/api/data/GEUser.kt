// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data

import com.intellij.collaboration.api.dto.GraphQLFragment
import com.intellij.openapi.util.NlsSafe
import cn.osc.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer

@GraphQLFragment("/graphql/fragment/userInfo.graphql")
class GEUser(id: String,
             @NlsSafe override val login: String,
             override val url: String,
             override val avatarUrl: String,
             @NlsSafe override val name: String?)
  : GENode(id), GEActor, GEPullRequestRequestedReviewer {
  override val shortName: String = login
}