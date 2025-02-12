// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.ui.details.action

import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.pullrequest.ui.details.GEPRStateModel
import java.awt.event.ActionEvent

internal class GEPRCommitMergeAction(stateModel: GEPRStateModel)
  : GEPRMergeAction(GiteeBundle.message("pull.request.merge.commit.action"), stateModel) {

  override fun actionPerformed(e: ActionEvent?) = stateModel.submitMergeTask()
}