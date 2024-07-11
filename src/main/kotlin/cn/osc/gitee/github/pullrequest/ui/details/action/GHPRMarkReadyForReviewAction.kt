// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.ui.details.action

import cn.osc.gitee.github.i18n.GithubBundle
import cn.osc.gitee.github.pullrequest.ui.details.GHPRStateModel
import java.awt.event.ActionEvent

internal class GHPRMarkReadyForReviewAction(stateModel: GHPRStateModel)
  : GHPRStateChangeAction(GithubBundle.message("pull.request.mark.ready.for.review"), stateModel) {

  override fun actionPerformed(e: ActionEvent?) = stateModel.submitMarkReadyForReviewTask()
}