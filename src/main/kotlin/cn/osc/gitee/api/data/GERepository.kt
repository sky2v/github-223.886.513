// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.api.data

import com.intellij.collaboration.api.dto.GraphQLFragment
import cn.osc.gitee.api.GERepositoryPath
import cn.osc.gitee.api.data.pullrequest.GEGitRefName

@GraphQLFragment("/graphql/fragment/repositoryInfo.graphql")
class GERepository(id: String,
                   val owner: GERepositoryOwnerName,
                   nameWithOwner: String,
                   val viewerPermission: GERepositoryPermissionLevel?,
                   val mergeCommitAllowed: Boolean,
                   val squashMergeAllowed: Boolean,
                   val rebaseMergeAllowed: Boolean,
                   val defaultBranchRef: GEGitRefName?,
                   val isFork: Boolean)
  : GENode(id) {
  val path: GERepositoryPath
  val defaultBranch = defaultBranchRef?.name

  init {
    val split = nameWithOwner.split('/')
    path = GERepositoryPath(split[0], split[1])
  }
}