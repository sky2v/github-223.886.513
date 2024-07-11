// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.ui.details

import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.openapi.progress.ProgressIndicator
import cn.osc.gitee.api.data.GELabel
import cn.osc.gitee.api.data.GERepositoryPermissionLevel
import cn.osc.gitee.api.data.GEUser
import cn.osc.gitee.api.data.pullrequest.GEPullRequest
import cn.osc.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import cn.osc.gitee.pullrequest.data.provider.GEPRDetailsDataProvider
import cn.osc.gitee.pullrequest.data.service.GEPRRepositoryDataService
import cn.osc.gitee.pullrequest.data.service.GEPRSecurityService
import com.intellij.collaboration.util.CollectionDelta

class GEPRMetadataModelImpl(private val valueModel: SingleValueModel<GEPullRequest>,
                            securityService: GEPRSecurityService,
                            repositoryDataService: GEPRRepositoryDataService,
                            private val detailsDataProvider: GEPRDetailsDataProvider) : GEPRMetadataModelBase(repositoryDataService) {

  override val assignees: List<GEUser>
    get() = valueModel.value.assignees
  override val reviewers: List<GEPullRequestRequestedReviewer>
    get() = valueModel.value.reviewRequests.mapNotNull { it.requestedReviewer }
  override val labels: List<GELabel>
    get() = valueModel.value.labels

  override fun getAuthor() = valueModel.value.author as? GEUser

  override val isEditingAllowed = securityService.currentUserHasPermissionLevel(GERepositoryPermissionLevel.TRIAGE)

  override fun adjustReviewers(indicator: ProgressIndicator, delta: CollectionDelta<GEPullRequestRequestedReviewer>) =
    detailsDataProvider.adjustReviewers(indicator, delta)

  override fun adjustAssignees(indicator: ProgressIndicator, delta: CollectionDelta<GEUser>) =
    detailsDataProvider.adjustAssignees(indicator, delta)

  override fun adjustLabels(indicator: ProgressIndicator, delta: CollectionDelta<GELabel>) =
    detailsDataProvider.adjustLabels(indicator, delta)

  override fun addAndInvokeChangesListener(listener: () -> Unit) =
    valueModel.addAndInvokeListener { listener() }
}