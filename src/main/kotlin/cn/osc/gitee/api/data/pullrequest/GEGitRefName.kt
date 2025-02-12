// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data.pullrequest

import com.intellij.collaboration.api.dto.GraphQLFragment

@GraphQLFragment("/graphql/fragment/gitRefName.graphql")
class GEGitRefName(val name: String)