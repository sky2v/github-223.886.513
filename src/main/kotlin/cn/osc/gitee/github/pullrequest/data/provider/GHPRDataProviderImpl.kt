// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.data.provider

import com.intellij.openapi.Disposable
import cn.osc.gitee.github.api.data.pullrequest.timeline.GHPRTimelineItem
import cn.osc.gitee.github.pullrequest.GHPRDiffRequestModel
import cn.osc.gitee.github.pullrequest.data.GHListLoader
import cn.osc.gitee.github.pullrequest.data.GHPRIdentifier
import cn.osc.gitee.github.util.DisposalCountingHolder

internal class GHPRDataProviderImpl(override val id: GHPRIdentifier,
                                    override val detailsData: GHPRDetailsDataProvider,
                                    override val stateData: GHPRStateDataProvider,
                                    override val changesData: GHPRChangesDataProvider,
                                    override val commentsData: GHPRCommentsDataProvider,
                                    override val reviewData: GHPRReviewDataProvider,
                                    override val viewedStateData: GHPRViewedStateDataProvider,
                                    private val timelineLoaderHolder: DisposalCountingHolder<GHListLoader<GHPRTimelineItem>>,
                                    override val diffRequestModel: GHPRDiffRequestModel)
  : GHPRDataProvider {

  override val timelineLoader get() = timelineLoaderHolder.value

  override fun acquireTimelineLoader(disposable: Disposable) =
    timelineLoaderHolder.acquireValue(disposable)
}