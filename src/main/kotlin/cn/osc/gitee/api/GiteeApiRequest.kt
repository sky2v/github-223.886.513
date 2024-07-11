// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.collaboration.api.dto.GraphQLRequestDTO
import com.intellij.collaboration.api.dto.GraphQLResponseDTO
import com.intellij.collaboration.api.util.LinkHttpHeaderValue
import com.intellij.util.ThrowableConvertor
import cn.osc.gitee.api.data.GiteeResponsePage
import cn.osc.gitee.api.data.GiteeSearchResult
import cn.osc.gitee.api.data.graphql.GEGQLError
import cn.osc.gitee.exceptions.GiteeAuthenticationException
import cn.osc.gitee.exceptions.GiteeConfusingException
import cn.osc.gitee.exceptions.GiteeJsonException
import java.io.IOException

/**
 * Represents an API request with strictly defined response type
 */
sealed class GiteeApiRequest<out T>(val url: String) {
  var operationName: String? = null
  abstract val acceptMimeType: String?

  protected val headers = mutableMapOf<String, String>()
  val additionalHeaders: Map<String, String>
    get() = headers

  @Throws(IOException::class)
  abstract fun extractResult(response: GiteeApiResponse): T

  fun withOperationName(name: String): GiteeApiRequest<T> {
    operationName = name
    return this
  }

  abstract class Get<T> @JvmOverloads constructor(url: String,
                                                  override val acceptMimeType: String? = null) : GiteeApiRequest<T>(url) {
    abstract class Optional<T> @JvmOverloads constructor(url: String,
                                                         acceptMimeType: String? = null) : Get<T?>(url, acceptMimeType) {
      companion object {
        inline fun <reified T> json(url: String, acceptMimeType: String? = null): Optional<T> =
          Json(url, T::class.java, acceptMimeType)
      }

      open class Json<T>(url: String, private val clazz: Class<T>, acceptMimeType: String? = GiteeApiContentHelper.V3_JSON_MIME_TYPE)
        : Optional<T>(url, acceptMimeType) {

        override fun extractResult(response: GiteeApiResponse): T = parseJsonObject(response, clazz)
      }
    }

    companion object {
      inline fun <reified T> json(url: String, acceptMimeType: String? = null): Get<T> =
        Json(url, T::class.java, acceptMimeType)

      inline fun <reified T> jsonPage(url: String, acceptMimeType: String? = null): Get<GiteeResponsePage<T>> =
        JsonPage(url, T::class.java, acceptMimeType)

      inline fun <reified T> jsonSearchPage(url: String, acceptMimeType: String? = null): Get<GiteeResponsePage<T>> =
        JsonSearchPage(url, T::class.java, acceptMimeType)
    }

    open class Json<T>(url: String, private val clazz: Class<T>, acceptMimeType: String? = GiteeApiContentHelper.V3_JSON_MIME_TYPE)
      : Get<T>(url, acceptMimeType) {

      override fun extractResult(response: GiteeApiResponse): T = parseJsonObject(response, clazz)
    }

    open class JsonList<T>(url: String, private val clazz: Class<T>, acceptMimeType: String? = GiteeApiContentHelper.V3_JSON_MIME_TYPE)
      : Get<List<T>>(url, acceptMimeType) {

      override fun extractResult(response: GiteeApiResponse): List<T> = parseJsonList(response, clazz)
    }

    open class JsonPage<T>(url: String, private val clazz: Class<T>, acceptMimeType: String? = GiteeApiContentHelper.V3_JSON_MIME_TYPE)
      : Get<GiteeResponsePage<T>>(url, acceptMimeType) {

      override fun extractResult(response: GiteeApiResponse): GiteeResponsePage<T> {
        val list = parseJsonList(response, clazz)
        val linkHeader = response.findHeader(LinkHttpHeaderValue.HEADER_NAME)?.let(LinkHttpHeaderValue::parse)
        return GiteeResponsePage(list, linkHeader)
      }
    }

    open class JsonSearchPage<T>(url: String,
                                 private val clazz: Class<T>,
                                 acceptMimeType: String? = GiteeApiContentHelper.V3_JSON_MIME_TYPE)
      : Get<GiteeResponsePage<T>>(url, acceptMimeType) {

      override fun extractResult(response: GiteeApiResponse): GiteeResponsePage<T> {
        val page = parseJsonSearchPage(response, clazz)
        val linkHeader = response.findHeader(LinkHttpHeaderValue.HEADER_NAME)?.let(LinkHttpHeaderValue::parse)
        return GiteeResponsePage(page.items, linkHeader)
      }
    }
  }

  abstract class Head<T> @JvmOverloads constructor(url: String,
                                                   override val acceptMimeType: String? = null) : GiteeApiRequest<T>(url)

  abstract class WithBody<out T>(url: String) : GiteeApiRequest<T>(url) {
    abstract val body: String?
    abstract val bodyMimeType: String
  }

  abstract class Post<out T> @JvmOverloads constructor(override val bodyMimeType: String,
                                                       url: String,
                                                       override var acceptMimeType: String? = null) : GiteeApiRequest.WithBody<T>(url) {
    companion object {
      inline fun <reified T> json(url: String, body: Any, acceptMimeType: String? = null): Post<T> =
        Json(url, body, T::class.java, acceptMimeType)
    }

    open class Json<T>(url: String, private val bodyObject: Any, private val clazz: Class<T>,
                       acceptMimeType: String? = GiteeApiContentHelper.V3_JSON_MIME_TYPE)
      : Post<T>(GiteeApiContentHelper.JSON_MIME_TYPE, url, acceptMimeType) {

      override val body: String
        get() = GiteeApiContentHelper.toJson(bodyObject)

      override fun extractResult(response: GiteeApiResponse): T = parseJsonObject(response, clazz)
    }

    abstract class GQLQuery<out T>(url: String,
                                   private val queryName: String,
                                   private val variablesObject: Any)
      : Post<T>(GiteeApiContentHelper.JSON_MIME_TYPE, url) {

      override val body: String
        get() {
          val query = GEGQLQueryLoader.loadQuery(queryName)
          val request = GraphQLRequestDTO(query, variablesObject)
          return GiteeApiContentHelper.toJson(request, true)
        }

      protected fun throwException(errors: List<GEGQLError>): Nothing {
        if (errors.any { it.type.equals("INSUFFICIENT_SCOPES", true) })
          throw GiteeAuthenticationException("Access token has not been granted the required scopes.")

        if (errors.size == 1) throw GiteeConfusingException(errors.single().toString())
        throw GiteeConfusingException(errors.toString())
      }

      class Parsed<out T>(url: String,
                          requestFilePath: String,
                          variablesObject: Any,
                          private val clazz: Class<T>)
        : GQLQuery<T>(url, requestFilePath, variablesObject) {
        override fun extractResult(response: GiteeApiResponse): T {
          val result: GraphQLResponseDTO<out T, GEGQLError> = parseGQLResponse(response, clazz)
          val data = result.data
          if (data != null) return data

          val errors = result.errors
          if (errors == null) error("Undefined request state - both result and errors are null")
          else throwException(errors)
        }
      }

      class TraversedParsed<out T : Any>(url: String,
                                         requestFilePath: String,
                                         variablesObject: Any,
                                         private val clazz: Class<out T>,
                                         private vararg val pathFromData: String)
        : GQLQuery<T>(url, requestFilePath, variablesObject) {

        override fun extractResult(response: GiteeApiResponse): T {
          return parseResponse(response, clazz, pathFromData)
                 ?: throw GiteeJsonException("Non-nullable entity is null or entity path is invalid")
        }
      }

      class OptionalTraversedParsed<T>(url: String,
                                       requestFilePath: String,
                                       variablesObject: Any,
                                       private val clazz: Class<T>,
                                       private vararg val pathFromData: String)
        : GQLQuery<T?>(url, requestFilePath, variablesObject) {
        override fun extractResult(response: GiteeApiResponse): T? {
          return parseResponse(response, clazz, pathFromData)
        }
      }

      internal fun <T> parseResponse(response: GiteeApiResponse,
                                     clazz: Class<T>,
                                     pathFromData: Array<out String>): T? {
        val result: GraphQLResponseDTO<out JsonNode, GEGQLError> = parseGQLResponse(response, JsonNode::class.java)
        val data = result.data
        if (data != null && !data.isNull) {
          var node: JsonNode = data
          for (path in pathFromData) {
            node = node[path] ?: break
          }
          if (!node.isNull) return GiteeApiContentHelper.fromJson(node.toString(), clazz, true)
        }
        val errors = result.errors
        if (errors == null) return null
        else throwException(errors)
      }
    }
  }

  abstract class Put<T> @JvmOverloads constructor(override val bodyMimeType: String,
                                                  url: String,
                                                  override val acceptMimeType: String? = null) : GiteeApiRequest.WithBody<T>(url) {
    companion object {
      inline fun <reified T> json(url: String, body: Any? = null): Put<T> = Json(url, body, T::class.java)

      inline fun <reified T> jsonList(url: String, body: Any): Put<List<T>> = JsonList(url, body, T::class.java)
    }

    open class Json<T>(url: String, private val bodyObject: Any?, private val clazz: Class<T>)
      : Put<T>(GiteeApiContentHelper.JSON_MIME_TYPE, url, GiteeApiContentHelper.V3_JSON_MIME_TYPE) {
      init {
        if (bodyObject == null) headers["Content-Length"] = "0"
      }

      override val body: String?
        get() = bodyObject?.let { GiteeApiContentHelper.toJson(it) }

      override fun extractResult(response: GiteeApiResponse): T = parseJsonObject(response, clazz)
    }

    open class JsonList<T>(url: String, private val bodyObject: Any?, private val clazz: Class<T>)
      : Put<List<T>>(GiteeApiContentHelper.JSON_MIME_TYPE, url, GiteeApiContentHelper.V3_JSON_MIME_TYPE) {
      init {
        if (bodyObject == null) headers["Content-Length"] = "0"
      }

      override val body: String?
        get() = bodyObject?.let { GiteeApiContentHelper.toJson(it) }

      override fun extractResult(response: GiteeApiResponse): List<T> = parseJsonList(response, clazz)
    }
  }

  abstract class Patch<T> @JvmOverloads constructor(override val bodyMimeType: String,
                                                    url: String,
                                                    override var acceptMimeType: String? = null) : Post<T>(bodyMimeType,
                                                                                                           url,
                                                                                                           acceptMimeType) {
    companion object {
      inline fun <reified T> json(url: String, body: Any): Post<T> = Json(url, body, T::class.java)
    }

    open class Json<T>(url: String, bodyObject: Any, clazz: Class<T>) : Post.Json<T>(url, bodyObject, clazz)
  }

  abstract class Delete<T> @JvmOverloads constructor(override val bodyMimeType: String,
                                                     url: String,
                                                     override val acceptMimeType: String? = null) : GiteeApiRequest.WithBody<T>(url) {

    companion object {
      inline fun <reified T> json(url: String, body: Any? = null): Delete<T> = Json(url, body, T::class.java)
    }

    open class Json<T>(url: String, private val bodyObject: Any? = null, private val clazz: Class<T>)
      : Delete<T>(GiteeApiContentHelper.JSON_MIME_TYPE, url, GiteeApiContentHelper.V3_JSON_MIME_TYPE) {
      init {
        if (bodyObject == null) headers["Content-Length"] = "0"
      }

      override val body: String?
        get() = bodyObject?.let { GiteeApiContentHelper.toJson(it) }

      override fun extractResult(response: GiteeApiResponse): T = parseJsonObject(response, clazz)
    }
  }

  companion object {
    private fun <T> parseJsonObject(response: GiteeApiResponse, clazz: Class<T>): T {
      return response.readBody(ThrowableConvertor { GiteeApiContentHelper.readJsonObject(it, clazz) })
    }

    private fun <T> parseJsonList(response: GiteeApiResponse, clazz: Class<T>): List<T> {
      return response.readBody(ThrowableConvertor { GiteeApiContentHelper.readJsonList(it, clazz) })
    }

    private fun <T> parseJsonSearchPage(response: GiteeApiResponse, clazz: Class<T>): GiteeSearchResult<T> {
      return response.readBody(ThrowableConvertor {
        @Suppress("UNCHECKED_CAST")
        GiteeApiContentHelper.readJsonObject(it, GiteeSearchResult::class.java, clazz) as GiteeSearchResult<T>
      })
    }

    private fun <T> parseGQLResponse(response: GiteeApiResponse, dataClass: Class<out T>): GraphQLResponseDTO<out T, GEGQLError> {
      return response.readBody(ThrowableConvertor {
        @Suppress("UNCHECKED_CAST")
        GiteeApiContentHelper.readJsonObject(it, GraphQLResponseDTO::class.java, dataClass, GEGQLError::class.java,
                                              gqlNaming = true) as GraphQLResponseDTO<T, GEGQLError>
      })
    }
  }
}