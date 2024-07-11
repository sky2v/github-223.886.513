// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.ui.timeline

import cn.osc.gitee.api.data.pullrequest.timeline.GEPRTimelineEvent
import cn.osc.gitee.pullrequest.ui.timeline.GEPRTimelineItemComponentFactory.Item

interface GEPRTimelineEventComponentFactory<T : GEPRTimelineEvent> {
  fun createComponent(event: T): Item
}
