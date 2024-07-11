// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.authentication.accounts

import cn.osc.gitee.authentication.GEAccountsUtil

// Helper to hide account ID which is used to store account selection
object GEAccountSerializer {
  fun serialize(account: GiteeAccount): String = account.id
  fun deserialize(string: String): GiteeAccount? {
    return GEAccountsUtil.accounts.find { it.id == string }
  }
}