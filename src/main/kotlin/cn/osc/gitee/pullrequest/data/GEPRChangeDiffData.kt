// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data

import com.intellij.diff.util.Range
import com.intellij.diff.util.Side
import com.intellij.openapi.diff.impl.patch.TextFilePatch
import cn.osc.gitee.util.GEPatchHunkUtil

sealed class GEPRChangeDiffData(val commitSha: String, val filePath: String,
                                private val patch: TextFilePatch, private val cumulativePatch: TextFilePatch,
                                protected val fileHistory: GEPRFileHistory) {

  val diffRanges: List<Range> by lazy(LazyThreadSafetyMode.NONE) {
    patch.hunks.map(GEPatchHunkUtil::getRange)
  }
  val diffRangesWithoutContext: List<Range> by lazy(LazyThreadSafetyMode.NONE) {
    patch.hunks.map(GEPatchHunkUtil::getChangeOnlyRanges).flatten()
  }
  val linesMapper: GEPRChangedFileLinesMapper by lazy(LazyThreadSafetyMode.NONE) {
    GEPRChangedFileLinesMapperImpl(cumulativePatch)
  }

  fun contains(commitSha: String, filePath: String): Boolean {
    return fileHistory.contains(commitSha, filePath)
  }

  class Commit(commitSha: String, filePath: String,
               patch: TextFilePatch, cumulativePatch: TextFilePatch,
               fileHistory: GEPRFileHistory)
    : GEPRChangeDiffData(commitSha, filePath,
                         patch, cumulativePatch,
                         fileHistory) {

    fun mapPosition(fromCommitSha: String,
                    side: Side, line: Int): Pair<Side, Int>? {

      val comparison = fileHistory.compare(fromCommitSha, commitSha)
      if (comparison == 0) return side to line
      if (comparison < 0) {
        val patches = fileHistory.getPatches(fromCommitSha, commitSha, false, true)
        return transferLine(patches, side, line, false)
      }
      else {
        val patches = fileHistory.getPatches(commitSha, fromCommitSha, true, false)
        return transferLine(patches, side, line, true)
      }
    }

    private fun transferLine(patchChain: List<TextFilePatch>, side: Side, line: Int, rightToLeft: Boolean): Pair<Side, Int>? {
      // points to the same patch
      if (patchChain.isEmpty()) return side to line

      val patches = if (rightToLeft) patchChain.asReversed() else patchChain
      val transferFrom = if (rightToLeft) Side.RIGHT else Side.LEFT

      var currentSide: Side = side
      var currentLine: Int = line

      for (patch in patches) {
        if (currentSide == transferFrom) {
          val changeOnlyRanges = patch.hunks.map { hunk ->
            val ranges = GEPatchHunkUtil.getChangeOnlyRanges(hunk)
            if (rightToLeft) ranges.map { reverseRange(it) } else ranges
          }.flatten()

          var offset = 0
          loop@ for (range in changeOnlyRanges) {
            when {
              currentLine < range.start1 ->
                break@loop
              currentLine in range.start1 until range.end1 ->
                return null
              currentLine >= range.end1 ->
                offset += (range.end2 - range.start2) - (range.end1 - range.start1)
            }
          }
          currentLine += offset
        }
        else {
          currentSide = transferFrom
        }
      }
      return currentSide to currentLine
    }

    private fun reverseRange(range: Range) = Range(range.start2, range.end2, range.start1, range.end1)
  }

  class Cumulative(commitSha: String, filePath: String,
                   patch: TextFilePatch,
                   fileHistory: GEPRFileHistory)
    : GEPRChangeDiffData(commitSha, filePath,
                         patch, patch,
                         fileHistory)
}