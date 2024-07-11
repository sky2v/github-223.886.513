// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.ui

import javax.swing.Action

interface GHLoadingErrorHandler {
  fun getActionForError(error: Throwable): Action?
}
