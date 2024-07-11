// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data.pullrequest

import cn.osc.gitee.api.data.GECommitWithCheckStatuses
import cn.osc.gitee.api.data.GENode

class GEPullRequestCommitWithCheckStatuses(id: String, val commit: GECommitWithCheckStatuses) : GENode(id)