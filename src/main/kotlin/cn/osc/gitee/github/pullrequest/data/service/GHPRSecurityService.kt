// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.data.service

import cn.osc.gitee.github.api.data.GHRepositoryPermissionLevel
import cn.osc.gitee.github.api.data.GHUser
import cn.osc.gitee.github.api.data.GithubUser
import cn.osc.gitee.github.authentication.accounts.GithubAccount

interface GHPRSecurityService {
  val account: GithubAccount
  val currentUser: GHUser

  fun isCurrentUser(user: GithubUser): Boolean

  fun currentUserHasPermissionLevel(level: GHRepositoryPermissionLevel): Boolean

  fun isMergeAllowed(): Boolean
  fun isRebaseMergeAllowed(): Boolean
  fun isSquashMergeAllowed(): Boolean

  fun isMergeForbiddenForProject(): Boolean
  fun isUserInAnyTeam(slugs: List<String>): Boolean
}