// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.comment

import com.intellij.diff.util.Side
import cn.osc.gitee.api.data.pullrequest.GEPullRequestReviewThread

class GEPRDiffReviewThreadMapping(val diffSide: Side, val fileLineIndex: Int,
                                  val thread: GEPullRequestReviewThread)