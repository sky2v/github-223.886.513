// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data

import com.intellij.collaboration.api.dto.GraphQLFragment
import com.intellij.openapi.util.NlsSafe

@GraphQLFragment("/graphql/fragment/commitHash.graphql")
open class GECommitHash(id: String,
                        @NlsSafe val oid: String,
                        @NlsSafe val abbreviatedOid: String)
  : GENode(id)