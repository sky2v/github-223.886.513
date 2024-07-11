// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.tasks

import com.intellij.openapi.project.Project
import cn.osc.gitee.api.GiteeServerPath
import cn.osc.gitee.authentication.GEAccountsUtil
import cn.osc.gitee.authentication.GELoginRequest
import cn.osc.gitee.authentication.ui.GELoginModel
import cn.osc.gitee.exceptions.GiteeParseException

private object GERepositoryEditorKt {
  fun askToken(project: Project, host: String): String? {
    val server = tryParse(host) ?: return null

    val model = object : GELoginModel {
      var token: String? = null

      override fun isAccountUnique(server: GiteeServerPath, login: String): Boolean = true

      override suspend fun saveLogin(server: GiteeServerPath, login: String, token: String) {
        this.token = token
      }
    }
    GEAccountsUtil.login(model, GELoginRequest(server = server), project, null)
    return model.token
  }

  private fun tryParse(host: String): GiteeServerPath? {
    return try {
      GiteeServerPath.from(host)
    }
    catch (ignored: GiteeParseException) {
      null
    }
  }
}