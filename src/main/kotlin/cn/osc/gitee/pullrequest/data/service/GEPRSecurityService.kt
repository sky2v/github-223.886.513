// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data.service

import cn.osc.gitee.api.data.GERepositoryPermissionLevel
import cn.osc.gitee.api.data.GEUser
import cn.osc.gitee.api.data.GiteeUser
import cn.osc.gitee.authentication.accounts.GiteeAccount

interface GEPRSecurityService {
  val account: GiteeAccount
  val currentUser: GEUser

  fun isCurrentUser(user: GiteeUser): Boolean

  fun currentUserHasPermissionLevel(level: GERepositoryPermissionLevel): Boolean

  fun isMergeAllowed(): Boolean
  fun isRebaseMergeAllowed(): Boolean
  fun isSquashMergeAllowed(): Boolean

  fun isMergeForbiddenForProject(): Boolean
  fun isUserInAnyTeam(slugs: List<String>): Boolean
}