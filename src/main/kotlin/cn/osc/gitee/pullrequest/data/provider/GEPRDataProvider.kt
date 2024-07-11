// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data.provider

import com.intellij.openapi.Disposable
import com.intellij.util.concurrency.annotations.RequiresEdt
import cn.osc.gitee.api.data.pullrequest.timeline.GEPRTimelineItem
import cn.osc.gitee.pullrequest.GEPRDiffRequestModel
import cn.osc.gitee.pullrequest.data.GEListLoader
import cn.osc.gitee.pullrequest.data.GEPRIdentifier

interface GEPRDataProvider {
  val id: GEPRIdentifier
  val detailsData: GEPRDetailsDataProvider
  val stateData: GEPRStateDataProvider
  val changesData: GEPRChangesDataProvider
  val commentsData: GEPRCommentsDataProvider
  val reviewData: GEPRReviewDataProvider
  val viewedStateData: GEPRViewedStateDataProvider
  val timelineLoader: GEListLoader<GEPRTimelineItem>?
  val diffRequestModel: GEPRDiffRequestModel

  @RequiresEdt
  fun acquireTimelineLoader(disposable: Disposable): GEListLoader<GEPRTimelineItem>
}