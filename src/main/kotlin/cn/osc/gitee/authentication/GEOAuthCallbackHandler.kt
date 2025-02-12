// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.authentication

import com.intellij.collaboration.auth.OAuthCallbackHandlerBase
import com.intellij.collaboration.auth.services.OAuthService

internal class GEOAuthCallbackHandler : OAuthCallbackHandlerBase() {
  override fun oauthService(): OAuthService<*> = GEOAuthService.instance

  override fun handleAcceptCode(isAccepted: Boolean): AcceptCodeHandleResult {
    val redirectUrl = if (isAccepted) {
      GEOAuthService.SERVICE_URL.resolve("complete")
    }
    else {
      GEOAuthService.SERVICE_URL.resolve("error")
    }
    return AcceptCodeHandleResult.Redirect(redirectUrl)
  }
}
