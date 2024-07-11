// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.github.pullrequest.data

import cn.osc.gitee.github.api.data.GHNode

class SimpleGHPRIdentifier(id: String, override val number: Long) : GHNode(id), GHPRIdentifier {
  constructor(id: GHPRIdentifier) : this(id.id, id.number)
}
