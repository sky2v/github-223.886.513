// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.exceptions;

import org.jetbrains.annotations.NotNull;
import cn.osc.gitee.authentication.accounts.GiteeAccount;

public class GiteeMissingTokenException extends GiteeAuthenticationException {
  public GiteeMissingTokenException(@NotNull String message) {
    super(message);
  }

  public GiteeMissingTokenException(@NotNull GiteeAccount account) {
    this("Missing access token for account " + account);
  }
}
