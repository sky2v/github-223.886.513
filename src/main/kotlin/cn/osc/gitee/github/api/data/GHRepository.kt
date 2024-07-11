// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.osc.gitee.github.api.data

import com.intellij.collaboration.api.dto.GraphQLFragment
import cn.osc.gitee.github.api.GHRepositoryPath
import cn.osc.gitee.github.api.data.pullrequest.GHGitRefName

@GraphQLFragment("/graphql/fragment/repositoryInfo.graphql")
class GHRepository(id: String,
                   val owner: GHRepositoryOwnerName,
                   nameWithOwner: String,
                   val viewerPermission: GHRepositoryPermissionLevel?,
                   val mergeCommitAllowed: Boolean,
                   val squashMergeAllowed: Boolean,
                   val rebaseMergeAllowed: Boolean,
                   val defaultBranchRef: GHGitRefName?,
                   val isFork: Boolean)
  : GHNode(id) {
  val path: GHRepositoryPath
  val defaultBranch = defaultBranchRef?.name

  init {
    val split = nameWithOwner.split('/')
    path = GHRepositoryPath(split[0], split[1])
  }
}