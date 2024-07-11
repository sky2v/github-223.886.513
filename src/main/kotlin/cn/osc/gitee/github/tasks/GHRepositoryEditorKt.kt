// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.tasks

import com.intellij.openapi.project.Project
import cn.osc.gitee.github.api.GithubServerPath
import cn.osc.gitee.github.authentication.GHAccountsUtil
import cn.osc.gitee.github.authentication.GHLoginRequest
import cn.osc.gitee.github.authentication.ui.GHLoginModel
import cn.osc.gitee.github.exceptions.GithubParseException

private object GHRepositoryEditorKt {
  fun askToken(project: Project, host: String): String? {
    val server = tryParse(host) ?: return null

    val model = object : GHLoginModel {
      var token: String? = null

      override fun isAccountUnique(server: GithubServerPath, login: String): Boolean = true

      override suspend fun saveLogin(server: GithubServerPath, login: String, token: String) {
        this.token = token
      }
    }
    GHAccountsUtil.login(model, GHLoginRequest(server = server), project, null)
    return model.token
  }

  private fun tryParse(host: String): GithubServerPath? {
    return try {
      GithubServerPath.from(host)
    }
    catch (ignored: GithubParseException) {
      null
    }
  }
}