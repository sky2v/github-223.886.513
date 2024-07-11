// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.ui.changes

import com.intellij.diff.util.Side
import cn.osc.gitee.pullrequest.data.GEPRChangedFileLinesMapper

class GEPRCreateDiffCommentParametersHelper(val commitSha: String, val filePath: String,
                                            private val linesMapper: GEPRChangedFileLinesMapper) {

  fun findPosition(diffSide: Side, sideFileLine: Int): Int? = linesMapper.findDiffLine(diffSide, sideFileLine)
}
