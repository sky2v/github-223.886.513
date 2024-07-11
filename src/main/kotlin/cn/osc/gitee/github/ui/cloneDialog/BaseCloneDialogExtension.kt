// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.ui.cloneDialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtension
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionStatusLine
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionStatusLine.Companion.greyText
import cn.osc.gitee.github.authentication.accounts.GithubAccount
import cn.osc.gitee.github.authentication.accounts.isGHAccount
import cn.osc.gitee.github.i18n.GithubBundle.message
import javax.swing.Icon

private val GithubAccount.nameWithServer: String
  get() {
    val serverPrefix = if (isGHAccount) "" else "${server.host}/"
    return serverPrefix + name
  }

abstract class BaseCloneDialogExtension : VcsCloneDialogExtension {
  override fun getIcon(): Icon = AllIcons.Vcs.Vendors.Github

  protected abstract fun getAccounts(): Collection<GithubAccount>

  override fun getAdditionalStatusLines(): List<VcsCloneDialogExtensionStatusLine> {
    val accounts = getAccounts()
    if (accounts.isEmpty()) return listOf(greyText(message("accounts.label.no.accounts")))

    return accounts.map { greyText(it.nameWithServer) }
  }
}