// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.api.data.pullrequest

import cn.osc.gitee.github.api.data.GHCommit
import cn.osc.gitee.github.api.data.GHNode

class GHPullRequestCommit(id: String,
                          val commit: GHCommit,
                          val url: String)
  : GHNode(id)