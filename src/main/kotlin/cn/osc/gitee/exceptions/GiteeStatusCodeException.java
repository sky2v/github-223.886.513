// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.exceptions;

import org.jetbrains.annotations.Nullable;
import cn.osc.gitee.api.data.GiteeErrorMessage;

public class GiteeStatusCodeException extends GiteeConfusingException {
  private final int myStatusCode;
  @Nullable private final GiteeErrorMessage myError;

  public GiteeStatusCodeException(@Nullable String message, int statusCode) {
    this(message, null, statusCode);
  }

  public GiteeStatusCodeException(@Nullable String message, @Nullable GiteeErrorMessage error, int statusCode) {
    super(message);
    myStatusCode = statusCode;
    myError = error;
  }

  public int getStatusCode() {
    return myStatusCode;
  }

  @Nullable
  public GiteeErrorMessage getError() {
    return myError;
  }
}
