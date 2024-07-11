// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.api.data.pullrequest.timeline

import com.fasterxml.jackson.annotation.JsonProperty
import cn.osc.gitee.github.api.data.GHActor
import java.util.*

class GHPRReviewDismissedEvent(override val actor: GHActor?,
                               override val createdAt: Date,
                               val dismissalMessageHTML: String?,
                               @JsonProperty("review") review: ReviewAuthor?)
  : GHPRTimelineEvent.Complex {

  val reviewAuthor = review?.author

  class ReviewAuthor(val author: GHActor?)
}
