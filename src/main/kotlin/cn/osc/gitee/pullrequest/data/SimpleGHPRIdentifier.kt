// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data

import cn.osc.gitee.api.data.GENode

class SimpleGHPRIdentifier(id: String, override val number: Long) : GENode(id), GEPRIdentifier {
  constructor(id: GEPRIdentifier) : this(id.id, id.number)
}
