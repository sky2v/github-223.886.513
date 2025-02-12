// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.ui.details

import cn.osc.gitee.api.data.GEUser
import cn.osc.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import cn.osc.gitee.pullrequest.data.service.GEPRRepositoryDataService
import java.util.concurrent.CompletableFuture

abstract class GEPRMetadataModelBase(private val repositoryDataService: GEPRRepositoryDataService) : GEPRMetadataModel {

  override fun loadPotentialReviewers(): CompletableFuture<List<GEPullRequestRequestedReviewer>> {
    val author = getAuthor()
    return repositoryDataService.potentialReviewers.thenApply { reviewers ->
      reviewers.mapNotNull { if (it == author) null else it }
    }
  }

  protected abstract fun getAuthor(): GEUser?

  override fun loadPotentialAssignees() = repositoryDataService.issuesAssignees
  override fun loadAssignableLabels() = repositoryDataService.labels
}