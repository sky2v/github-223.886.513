// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data.service

import cn.osc.gitee.api.data.GERepository
import cn.osc.gitee.api.data.GERepositoryPermissionLevel
import cn.osc.gitee.api.data.GEUser
import cn.osc.gitee.api.data.GiteeUser
import cn.osc.gitee.api.data.pullrequest.GETeam
import cn.osc.gitee.authentication.accounts.GiteeAccount
import cn.osc.gitee.util.GiteeSharedProjectSettings

class GEPRSecurityServiceImpl(private val sharedProjectSettings: GiteeSharedProjectSettings,
                              override val account: GiteeAccount,
                              override val currentUser: GEUser,
                              private val currentUserTeams: List<GETeam>,
                              private val repo: GERepository) : GEPRSecurityService {
  override fun isCurrentUser(user: GiteeUser) = user.nodeId == currentUser.id

  override fun currentUserHasPermissionLevel(level: GERepositoryPermissionLevel) =
    (repo.viewerPermission?.ordinal ?: -1) >= level.ordinal

  override fun isUserInAnyTeam(slugs: List<String>) = currentUserTeams.any { slugs.contains(it.slug) }

  override fun isMergeAllowed() = repo.mergeCommitAllowed
  override fun isRebaseMergeAllowed() = repo.rebaseMergeAllowed
  override fun isSquashMergeAllowed() = repo.squashMergeAllowed

  override fun isMergeForbiddenForProject() = sharedProjectSettings.pullRequestMergeForbidden
}