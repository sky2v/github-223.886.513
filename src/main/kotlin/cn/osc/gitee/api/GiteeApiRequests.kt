// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api

import com.intellij.util.ThrowableConvertor
import cn.osc.gitee.api.GiteeApiRequest.*
import cn.osc.gitee.api.data.*
import cn.osc.gitee.api.data.request.*
import cn.osc.gitee.api.util.GESchemaPreview
import cn.osc.gitee.api.util.GiteeApiPagesLoader
import cn.osc.gitee.api.util.GiteeApiSearchQueryBuilder
import cn.osc.gitee.api.util.GiteeApiUrlQueryBuilder
import java.awt.Image
import java.awt.image.BufferedImage

/**
 * Collection of factory methods for API requests used in plugin
 * TODO: improve url building (DSL?)
 */
object GiteeApiRequests {
  object CurrentUser : Entity("/user") {
    @JvmStatic
    fun get(server: GiteeServerPath) = get(getUrl(server, urlSuffix))

    @JvmStatic
    fun get(url: String) = Get.json<GiteeAuthenticatedUser>(url).withOperationName("get profile information")

    @JvmStatic
    fun getAvatar(url: String) = object : Get<BufferedImage>(url) {
      override fun extractResult(response: GiteeApiResponse): BufferedImage {
        return response.handleBody(ThrowableConvertor {
          GiteeApiContentHelper.loadImage(it)
        })
      }
    }.withOperationName("get profile avatar")

    object Repos : Entity("/repos") {
      @JvmOverloads
      @JvmStatic
      fun pages(server: GiteeServerPath,
                type: Type = Type.DEFAULT,
                visibility: Visibility = Visibility.DEFAULT,
                affiliation: Affiliation = Affiliation.DEFAULT,
                pagination: GiteeRequestPagination? = null) =
        GiteeApiPagesLoader.Request(get(server, type, visibility, affiliation, pagination), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath,
              type: Type = Type.DEFAULT,
              visibility: Visibility = Visibility.DEFAULT,
              affiliation: Affiliation = Affiliation.DEFAULT,
              pagination: GiteeRequestPagination? = null): GiteeApiRequest<GiteeResponsePage<GiteeRepo>> {
        if (type != Type.DEFAULT && (visibility != Visibility.DEFAULT || affiliation != Affiliation.DEFAULT)) {
          throw IllegalArgumentException("Param 'type' should not be used together with 'visibility' or 'affiliation'")
        }

        return get(getUrl(server, CurrentUser.urlSuffix, urlSuffix,
                          getQuery(type.toString(), visibility.toString(), affiliation.toString(), pagination?.toString().orEmpty())))
      }

      @JvmStatic
      fun get(url: String) = Get.jsonPage<GiteeRepo>(url).withOperationName("get user repositories")

      @JvmStatic
      fun create(server: GiteeServerPath, name: String, description: String, private: Boolean, autoInit: Boolean? = null) =
        Post.json<GiteeRepo>(getUrl(server, CurrentUser.urlSuffix, urlSuffix),
                              GiteeRepoRequest(name, description, private, autoInit))
          .withOperationName("create user repository")
    }

    object Orgs : Entity("/orgs") {
      @JvmOverloads
      @JvmStatic
      fun pages(server: GiteeServerPath, pagination: GiteeRequestPagination? = null) =
        GiteeApiPagesLoader.Request(get(server, pagination), ::get)

      fun get(server: GiteeServerPath, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, CurrentUser.urlSuffix, urlSuffix, getQuery(pagination?.toString().orEmpty())))

      fun get(url: String) = Get.jsonPage<GiteeOrg>(url).withOperationName("get user organizations")
    }

    object RepoSubs : Entity("/subscriptions") {
      @JvmStatic
      fun pages(server: GiteeServerPath) = GiteeApiPagesLoader.Request(get(server), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, CurrentUser.urlSuffix, urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = Get.jsonPage<GiteeRepo>(url).withOperationName("get repository subscriptions")
    }
  }

  object Organisations : Entity("/orgs") {

    object Repos : Entity("/repos") {
      @JvmStatic
      fun pages(server: GiteeServerPath, organisation: String, pagination: GiteeRequestPagination? = null) =
        GiteeApiPagesLoader.Request(get(server, organisation, pagination), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, organisation: String, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, Organisations.urlSuffix, "/", organisation, urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = Get.jsonPage<GiteeRepo>(url).withOperationName("get organisation repositories")

      @JvmStatic
      fun create(server: GiteeServerPath, organisation: String, name: String, description: String, private: Boolean) =
        Post.json<GiteeRepo>(getUrl(server, Organisations.urlSuffix, "/", organisation, urlSuffix),
                              GiteeRepoRequest(name, description, private, null))
          .withOperationName("create organisation repository")
    }
  }

  object Repos : Entity("/repos") {
    @JvmStatic
    fun get(server: GiteeServerPath, username: String, repoName: String) =
      Get.Optional.json<GiteeRepoDetailed>(getUrl(server, urlSuffix, "/$username/$repoName"))
        .withOperationName("get information for repository $username/$repoName")

    @JvmStatic
    fun get(url: String) = Get.Optional.json<GiteeRepoDetailed>(url).withOperationName("get information for repository $url")

    @JvmStatic
    fun delete(server: GiteeServerPath, username: String, repoName: String) =
      delete(getUrl(server, urlSuffix, "/$username/$repoName")).withOperationName("delete repository $username/$repoName")

    @JvmStatic
    fun delete(url: String) = Delete.json<Unit>(url).withOperationName("delete repository at $url")

    object Branches : Entity("/branches") {
      @JvmStatic
      fun pages(server: GiteeServerPath, username: String, repoName: String) =
        GiteeApiPagesLoader.Request(get(server, username, repoName), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = Get.jsonPage<GiteeBranch>(url).withOperationName("get branches")

      @JvmStatic
      fun getProtection(repository: GERepositoryCoordinates, branchName: String): GiteeApiRequest<GEBranchProtectionRules> =
        Get.json(getUrl(repository, urlSuffix, "/$branchName", "/protection"), GESchemaPreview.BRANCH_PROTECTION.mimeType)
    }

    object Commits : Entity("/commits") {

      @JvmStatic
      fun compare(repository: GERepositoryCoordinates, refA: String, refB: String) =
        Get.json<GECommitsCompareResult>(getUrl(repository, "/compare/$refA...$refB")).withOperationName("compare refs")

      @JvmStatic
      fun getDiff(repository: GERepositoryCoordinates, ref: String) =
        object : Get<String>(getUrl(repository, urlSuffix, "/$ref"),
                             GiteeApiContentHelper.V3_DIFF_JSON_MIME_TYPE) {
          override fun extractResult(response: GiteeApiResponse): String {
            return response.handleBody(ThrowableConvertor {
              it.reader().use { it.readText() }
            })
          }
        }.withOperationName("get diff for ref")

      @JvmStatic
      fun getDiff(repository: GERepositoryCoordinates, refA: String, refB: String) =
        object : Get<String>(getUrl(repository, "/compare/$refA...$refB"),
                             GiteeApiContentHelper.V3_DIFF_JSON_MIME_TYPE) {
          override fun extractResult(response: GiteeApiResponse): String {
            return response.handleBody(ThrowableConvertor {
              it.reader().use { it.readText() }
            })
          }
        }.withOperationName("get diff between refs")
    }

    object Forks : Entity("/forks") {

      @JvmStatic
      fun create(server: GiteeServerPath, username: String, repoName: String) =
        Post.json<GiteeRepo>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix), Any())
          .withOperationName("fork repository $username/$repoName for cuurent user")

      @JvmStatic
      fun pages(server: GiteeServerPath, username: String, repoName: String) =
        GiteeApiPagesLoader.Request(get(server, username, repoName), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = Get.jsonPage<GiteeRepo>(url).withOperationName("get forks")
    }

    object Assignees : Entity("/assignees") {

      @JvmStatic
      fun pages(server: GiteeServerPath, username: String, repoName: String) =
        GiteeApiPagesLoader.Request(get(server, username, repoName), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = Get.jsonPage<GiteeUser>(url).withOperationName("get assignees")
    }

    object Labels : Entity("/labels") {

      @JvmStatic
      fun pages(server: GiteeServerPath, username: String, repoName: String) =
        GiteeApiPagesLoader.Request(get(server, username, repoName), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = Get.jsonPage<GiteeIssueLabel>(url).withOperationName("get assignees")
    }

    object Collaborators : Entity("/collaborators") {

      @JvmStatic
      fun pages(server: GiteeServerPath, username: String, repoName: String) =
        GiteeApiPagesLoader.Request(get(server, username, repoName), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = Get.jsonPage<GiteeUserWithPermissions>(url).withOperationName("get collaborators")

      @JvmStatic
      fun add(server: GiteeServerPath, username: String, repoName: String, collaborator: String) =
        Put.json<Unit>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, "/", collaborator))
    }

    object Issues : Entity("/issues") {

      @JvmStatic
      fun create(server: GiteeServerPath,
                 username: String,
                 repoName: String,
                 title: String,
                 body: String? = null,
                 milestone: Long? = null,
                 labels: List<String>? = null,
                 assignees: List<String>? = null) =
        Post.json<GiteeIssue>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix),
                               GiteeCreateIssueRequest(title, body, milestone, labels, assignees))

      @JvmStatic
      fun pages(server: GiteeServerPath, username: String, repoName: String,
                state: String? = null, assignee: String? = null) = GiteeApiPagesLoader.Request(get(server, username, repoName,
                                                                                                    state, assignee), ::get)

      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String,
              state: String? = null, assignee: String? = null, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix,
                   GiteeApiUrlQueryBuilder.urlQuery { param("state", state); param("assignee", assignee); param(pagination) }))

      @JvmStatic
      fun get(url: String) = Get.jsonPage<GiteeIssue>(url).withOperationName("get issues in repository")

      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String, id: String) =
        Get.Optional.json<GiteeIssue>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, "/", id))

      @JvmStatic
      fun updateState(server: GiteeServerPath, username: String, repoName: String, id: String, open: Boolean) =
        Patch.json<GiteeIssue>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, "/", id),
                                GiteeChangeIssueStateRequest(if (open) "open" else "closed"))

      @JvmStatic
      fun updateAssignees(server: GiteeServerPath, username: String, repoName: String, id: String, assignees: Collection<String>) =
        Patch.json<GiteeIssue>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, "/", id),
                                GiteeAssigneesCollectionRequest(assignees))

      object Comments : Entity("/comments") {
        @JvmStatic
        fun create(repository: GERepositoryCoordinates, issueId: Long, body: String) =
          create(repository.serverPath, repository.repositoryPath.owner, repository.repositoryPath.repository, issueId.toString(), body)

        @JvmStatic
        fun create(server: GiteeServerPath, username: String, repoName: String, issueId: String, body: String) =
          Post.json<GiteeIssueCommentWithHtml>(
            getUrl(server, Repos.urlSuffix, "/$username/$repoName", Issues.urlSuffix, "/", issueId, urlSuffix),
            GiteeCreateIssueCommentRequest(body),
            GiteeApiContentHelper.V3_HTML_JSON_MIME_TYPE)

        @JvmStatic
        fun pages(server: GiteeServerPath, username: String, repoName: String, issueId: String) =
          GiteeApiPagesLoader.Request(get(server, username, repoName, issueId), ::get)

        @JvmStatic
        fun pages(url: String) = GiteeApiPagesLoader.Request(get(url), ::get)

        @JvmStatic
        fun get(server: GiteeServerPath, username: String, repoName: String, issueId: String,
                pagination: GiteeRequestPagination? = null) =
          get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", Issues.urlSuffix, "/", issueId, urlSuffix,
                     GiteeApiUrlQueryBuilder.urlQuery { param(pagination) }))

        @JvmStatic
        fun get(url: String) = Get.jsonPage<GiteeIssueCommentWithHtml>(url, GiteeApiContentHelper.V3_HTML_JSON_MIME_TYPE)
          .withOperationName("get comments for issue")
      }

      object Labels : Entity("/labels") {
        @JvmStatic
        fun replace(server: GiteeServerPath, username: String, repoName: String, issueId: String, labels: Collection<String>) =
          Put.jsonList<GiteeIssueLabel>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", Issues.urlSuffix, "/", issueId, urlSuffix),
                                         GiteeLabelsCollectionRequest(labels))
      }
    }

    object PullRequests : Entity("/pulls") {

      @JvmStatic
      fun create(server: GiteeServerPath,
                 username: String, repoName: String,
                 title: String, description: String, head: String, base: String) =
        Post.json<GiteePullRequestDetailed>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix),
                                             GiteePullRequestRequest(title, description, head, base))
          .withOperationName("create pull request in $username/$repoName")

      @JvmStatic
      fun update(serverPath: GiteeServerPath, username: String, repoName: String, number: Long,
                 title: String? = null,
                 body: String? = null,
                 state: GiteeIssueState? = null,
                 base: String? = null,
                 maintainerCanModify: Boolean? = null) =
        Patch.json<GiteePullRequestDetailed>(getUrl(serverPath, Repos.urlSuffix, "/$username/$repoName", urlSuffix, "/$number"),
                                              GiteePullUpdateRequest(title, body, state, base, maintainerCanModify))
          .withOperationName("update pull request $number")

      @JvmStatic
      fun update(url: String,
                 title: String? = null,
                 body: String? = null,
                 state: GiteeIssueState? = null,
                 base: String? = null,
                 maintainerCanModify: Boolean? = null) =
        Patch.json<GiteePullRequestDetailed>(url, GiteePullUpdateRequest(title, body, state, base, maintainerCanModify))
          .withOperationName("update pull request")

      @JvmStatic
      fun merge(server: GiteeServerPath, repoPath: GERepositoryPath, number: Long,
                commitSubject: String, commitBody: String, headSha: String) =
        Put.json<Unit>(getUrl(server, Repos.urlSuffix, "/$repoPath", urlSuffix, "/$number", "/merge"),
                       GiteePullRequestMergeRequest(commitSubject, commitBody, headSha, GiteePullRequestMergeMethod.merge))
          .withOperationName("merge pull request ${number}")

      @JvmStatic
      fun squashMerge(server: GiteeServerPath, repoPath: GERepositoryPath, number: Long,
                      commitSubject: String, commitBody: String, headSha: String) =
        Put.json<Unit>(getUrl(server, Repos.urlSuffix, "/$repoPath", urlSuffix, "/$number", "/merge"),
                       GiteePullRequestMergeRequest(commitSubject, commitBody, headSha, GiteePullRequestMergeMethod.squash))
          .withOperationName("squash and merge pull request ${number}")

      @JvmStatic
      fun rebaseMerge(server: GiteeServerPath, repoPath: GERepositoryPath, number: Long,
                      headSha: String) =
        Put.json<Unit>(getUrl(server, Repos.urlSuffix, "/$repoPath", urlSuffix, "/$number", "/merge"),
                       GiteePullRequestMergeRebaseRequest(headSha))
          .withOperationName("rebase and merge pull request ${number}")

      @JvmStatic
      fun getListETag(server: GiteeServerPath, repoPath: GERepositoryPath) =
        object : Get<String?>(getUrl(server, Repos.urlSuffix, "/$repoPath", urlSuffix,
                                     GiteeApiUrlQueryBuilder.urlQuery { param(GiteeRequestPagination(pageSize = 1)) })) {
          override fun extractResult(response: GiteeApiResponse) = response.findHeader("ETag")
        }.withOperationName("get pull request list ETag")

      object Reviewers : Entity("/requested_reviewers") {
        @JvmStatic
        fun add(server: GiteeServerPath, username: String, repoName: String, number: Long,
                reviewers: Collection<String>, teamReviewers: List<String>) =
          Post.json<Unit>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", PullRequests.urlSuffix, "/$number", urlSuffix),
                          GiteeReviewersCollectionRequest(reviewers, teamReviewers))

        @JvmStatic
        fun remove(server: GiteeServerPath, username: String, repoName: String, number: Long,
                   reviewers: Collection<String>, teamReviewers: List<String>) =
          Delete.json<Unit>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", PullRequests.urlSuffix, "/$number", urlSuffix),
                            GiteeReviewersCollectionRequest(reviewers, teamReviewers))
      }
    }
  }

  object Gists : Entity("/gists") {
    @JvmStatic
    fun create(server: GiteeServerPath,
               contents: List<GiteeGistRequest.FileContent>, description: String, public: Boolean) =
      Post.json<GiteeGist>(getUrl(server, urlSuffix),
                            GiteeGistRequest(contents, description, public))
        .withOperationName("create gist")

    @JvmStatic
    fun get(server: GiteeServerPath, id: String) = Get.Optional.json<GiteeGist>(getUrl(server, urlSuffix, "/$id"))
      .withOperationName("get gist $id")

    @JvmStatic
    fun delete(server: GiteeServerPath, id: String) = Delete.json<Unit>(getUrl(server, urlSuffix, "/$id"))
      .withOperationName("delete gist $id")
  }

  object Search : Entity("/search") {
    object Issues : Entity("/issues") {
      @JvmStatic
      fun pages(server: GiteeServerPath, repoPath: GERepositoryPath?, state: String?, assignee: String?, query: String?) =
        GiteeApiPagesLoader.Request(get(server, repoPath, state, assignee, query), ::get)

      @JvmStatic
      fun get(server: GiteeServerPath, repoPath: GERepositoryPath?, state: String?, assignee: String?, query: String?,
              pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, Search.urlSuffix, urlSuffix,
                   GiteeApiUrlQueryBuilder.urlQuery {
                     param("q", GiteeApiSearchQueryBuilder.searchQuery {
                       qualifier("repo", repoPath?.toString().orEmpty())
                       qualifier("state", state)
                       qualifier("assignee", assignee)
                       query(query)
                     })
                     param(pagination)
                   }))

      @JvmStatic
      fun get(server: GiteeServerPath, query: String, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, Search.urlSuffix, urlSuffix,
                   GiteeApiUrlQueryBuilder.urlQuery {
                     param("q", query)
                     param(pagination)
                   }))


      @JvmStatic
      fun get(url: String) = Get.jsonSearchPage<GiteeSearchedIssue>(url).withOperationName("search issues in repository")
    }
  }

  abstract class Entity(val urlSuffix: String)

  private fun getUrl(server: GiteeServerPath, suffix: String) = server.toApiUrl() + suffix

  private fun getUrl(repository: GERepositoryCoordinates, vararg suffixes: String) =
    getUrl(repository.serverPath, Repos.urlSuffix, "/", repository.repositoryPath.toString(), *suffixes)

  fun getUrl(server: GiteeServerPath, vararg suffixes: String) = StringBuilder(server.toApiUrl()).append(*suffixes).toString()

  private fun getQuery(vararg queryParts: String): String {
    val builder = StringBuilder()
    for (part in queryParts) {
      if (part.isEmpty()) continue
      if (builder.isEmpty()) builder.append("?")
      else builder.append("&")
      builder.append(part)
    }
    return builder.toString()
  }
}