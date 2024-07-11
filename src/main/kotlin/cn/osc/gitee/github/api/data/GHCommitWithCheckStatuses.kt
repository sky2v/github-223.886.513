// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.api.data

open class GHCommitWithCheckStatuses(id: String, oid: String, abbreviatedOid: String,
                                     val status: Status?,
                                     val checkSuites: GHNodes<GHCommitCheckSuiteStatus>)
  : GHCommitHash(id, oid, abbreviatedOid) {

  class Status(val contexts: List<GHCommitStatusContextStatus>)
}