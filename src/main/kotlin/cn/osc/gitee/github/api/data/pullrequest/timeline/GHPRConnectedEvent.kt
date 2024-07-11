// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.api.data.pullrequest.timeline

import cn.osc.gitee.github.api.data.GHActor
import java.util.*

class GHPRConnectedEvent(override val actor: GHActor?,
                         override val createdAt: Date,
                         val subject: GHPRReferencedSubject)
  : GHPRTimelineEvent.Complex