// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.ui.details

import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.openapi.progress.ProgressIndicator
import cn.osc.gitee.github.api.data.GHLabel
import cn.osc.gitee.github.api.data.GHRepositoryPermissionLevel
import cn.osc.gitee.github.api.data.GHUser
import cn.osc.gitee.github.api.data.pullrequest.GHPullRequest
import cn.osc.gitee.github.api.data.pullrequest.GHPullRequestRequestedReviewer
import cn.osc.gitee.github.pullrequest.data.provider.GHPRDetailsDataProvider
import cn.osc.gitee.github.pullrequest.data.service.GHPRRepositoryDataService
import cn.osc.gitee.github.pullrequest.data.service.GHPRSecurityService
import com.intellij.collaboration.util.CollectionDelta

class GHPRMetadataModelImpl(private val valueModel: SingleValueModel<GHPullRequest>,
                            securityService: GHPRSecurityService,
                            repositoryDataService: GHPRRepositoryDataService,
                            private val detailsDataProvider: GHPRDetailsDataProvider) : GHPRMetadataModelBase(repositoryDataService) {

  override val assignees: List<GHUser>
    get() = valueModel.value.assignees
  override val reviewers: List<GHPullRequestRequestedReviewer>
    get() = valueModel.value.reviewRequests.mapNotNull { it.requestedReviewer }
  override val labels: List<GHLabel>
    get() = valueModel.value.labels

  override fun getAuthor() = valueModel.value.author as? GHUser

  override val isEditingAllowed = securityService.currentUserHasPermissionLevel(GHRepositoryPermissionLevel.TRIAGE)

  override fun adjustReviewers(indicator: ProgressIndicator, delta: CollectionDelta<GHPullRequestRequestedReviewer>) =
    detailsDataProvider.adjustReviewers(indicator, delta)

  override fun adjustAssignees(indicator: ProgressIndicator, delta: CollectionDelta<GHUser>) =
    detailsDataProvider.adjustAssignees(indicator, delta)

  override fun adjustLabels(indicator: ProgressIndicator, delta: CollectionDelta<GHLabel>) =
    detailsDataProvider.adjustLabels(indicator, delta)

  override fun addAndInvokeChangesListener(listener: () -> Unit) =
    valueModel.addAndInvokeListener { listener() }
}