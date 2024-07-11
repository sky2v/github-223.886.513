// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.ui.timeline

import cn.osc.gitee.github.api.data.pullrequest.timeline.GHPRTimelineEvent
import cn.osc.gitee.github.pullrequest.ui.timeline.GHPRTimelineItemComponentFactory.Item

interface GHPRTimelineEventComponentFactory<T : GHPRTimelineEvent> {
  fun createComponent(event: T): Item
}
