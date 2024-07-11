// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api

import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.EventDispatcher
import com.intellij.util.ThrowableConvertor
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.HttpSecurityUtil
import com.intellij.util.io.RequestBuilder
import org.jetbrains.annotations.TestOnly
import cn.osc.gitee.api.data.GiteeErrorMessage
import cn.osc.gitee.exceptions.*
import cn.osc.gitee.util.GiteeSettings
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.net.HttpURLConnection
import java.util.*
import java.util.zip.GZIPInputStream

/**
 * Executes API requests taking care of authentication, headers, proxies, timeouts, etc.
 */
sealed class GiteeApiRequestExecutor {

  open fun addListener(disposable: Disposable, listener: () -> Unit) = Unit

  @RequiresBackgroundThread
  @Throws(IOException::class, ProcessCanceledException::class)
  abstract fun <T> execute(indicator: ProgressIndicator, request: GiteeApiRequest<T>): T

  @TestOnly
  @RequiresBackgroundThread
  @Throws(IOException::class, ProcessCanceledException::class)
  fun <T> execute(request: GiteeApiRequest<T>): T = execute(EmptyProgressIndicator(), request)

  internal class WithTokenAuth(githubSettings: GiteeSettings,
                               private val tokenSupplier: () -> String,
                               private val useProxy: Boolean) : Base(githubSettings) {

    @Throws(IOException::class, ProcessCanceledException::class)
    override fun <T> execute(indicator: ProgressIndicator, request: GiteeApiRequest<T>): T {
      check(!service<GERequestExecutorBreaker>().isRequestsShouldFail) {
        "Request failure was triggered by user action. This a pretty long description of this failure that should resemble some long error which can go out of bounds."
      }

      indicator.checkCanceled()
      return createRequestBuilder(request)
        .tuner { connection ->
          request.additionalHeaders.forEach(connection::addRequestProperty)
          connection.addRequestProperty(HttpSecurityUtil.AUTHORIZATION_HEADER_NAME, "Bearer ${tokenSupplier()}")
        }
        .useProxy(useProxy)
        .execute(request, indicator)
    }

    override fun addListener(disposable: Disposable, listener: () -> Unit) {
      if (tokenSupplier is MutableTokenSupplier) {
        tokenSupplier.addListener(disposable, listener)
      }
    }
  }

  internal class NoAuth(githubSettings: GiteeSettings) : Base(githubSettings) {
    override fun <T> execute(indicator: ProgressIndicator, request: GiteeApiRequest<T>): T {
      indicator.checkCanceled()
      return createRequestBuilder(request)
        .tuner { connection ->
          request.additionalHeaders.forEach(connection::addRequestProperty)
        }
        .useProxy(true)
        .execute(request, indicator)
    }
  }

  abstract class Base(private val githubSettings: GiteeSettings) : GiteeApiRequestExecutor() {
    protected fun <T> RequestBuilder.execute(request: GiteeApiRequest<T>, indicator: ProgressIndicator): T {
      indicator.checkCanceled()
      try {
        LOG.debug("Request: ${request.url} ${request.operationName} : Connecting")
        return connect {
          val connection = it.connection as HttpURLConnection
          if (request is GiteeApiRequest.WithBody) {
            LOG.debug("Request: ${connection.requestMethod} ${connection.url} with body:\n${request.body} : Connected")
            request.body?.let { body -> it.write(body) }
          }
          else {
            LOG.debug("Request: ${connection.requestMethod} ${connection.url} : Connected")
          }
          checkResponseCode(connection)
          checkServerVersion(connection)
          indicator.checkCanceled()
          val result = request.extractResult(createResponse(it, indicator))
          LOG.debug("Request: ${connection.requestMethod} ${connection.url} : Result extracted")
          result
        }
      }
      catch (e: GiteeStatusCodeException) {
        @Suppress("UNCHECKED_CAST")
        if (request is GiteeApiRequest.Get.Optional<*> && e.statusCode == HttpURLConnection.HTTP_NOT_FOUND) return null as T else throw e
      }
      catch (e: GiteeConfusingException) {
        if (request.operationName != null) {
          val errorText = "Can't ${request.operationName}"
          e.setDetails(errorText)
          LOG.debug(errorText, e)
        }
        throw e
      }
    }

    protected fun createRequestBuilder(request: GiteeApiRequest<*>): RequestBuilder {
      return when (request) {
        is GiteeApiRequest.Get -> HttpRequests.request(request.url)
        is GiteeApiRequest.Patch -> HttpRequests.patch(request.url, request.bodyMimeType)
        is GiteeApiRequest.Post -> HttpRequests.post(request.url, request.bodyMimeType)
        is GiteeApiRequest.Put -> HttpRequests.put(request.url, request.bodyMimeType)
        is GiteeApiRequest.Head -> HttpRequests.head(request.url)
        is GiteeApiRequest.Delete -> {
          if (request.body == null) HttpRequests.delete(request.url) else HttpRequests.delete(request.url, request.bodyMimeType)
        }

        else -> throw UnsupportedOperationException("${request.javaClass} is not supported")
      }
        .connectTimeout(githubSettings.connectionTimeout)
        .userAgent("Intellij IDEA Gitee Plugin")
        .throwStatusCodeException(false)
        .forceHttps(false)
        .accept(request.acceptMimeType)
    }

    @Throws(IOException::class)
    private fun checkResponseCode(connection: HttpURLConnection) {
      if (connection.responseCode < 400) return
      val statusLine = "${connection.responseCode} ${connection.responseMessage}"
      val errorText = getErrorText(connection)
      LOG.debug("Request: ${connection.requestMethod} ${connection.url} : Error ${statusLine} body:\n${errorText}")

      val jsonError = errorText?.let { getJsonError(connection, it) }
      jsonError ?: LOG.debug("Request: ${connection.requestMethod} ${connection.url} : Unable to parse JSON error")

      throw when (connection.responseCode) {
        HttpURLConnection.HTTP_UNAUTHORIZED,
        HttpURLConnection.HTTP_PAYMENT_REQUIRED,
        HttpURLConnection.HTTP_FORBIDDEN -> {
          if (jsonError?.containsReasonMessage("API rate limit exceeded") == true) {
            GiteeRateLimitExceededException(jsonError.presentableError)
          }
          else GiteeAuthenticationException("Request response: " + (jsonError?.presentableError ?: errorText ?: statusLine))
        }

        else -> {
          if (jsonError != null) {
            GiteeStatusCodeException("$statusLine - ${jsonError.presentableError}", jsonError, connection.responseCode)
          }
          else {
            GiteeStatusCodeException("$statusLine - ${errorText}", connection.responseCode)
          }
        }
      }
    }

    private fun checkServerVersion(connection: HttpURLConnection) {
      // let's assume it's not ghe if header is missing
      val versionHeader = connection.getHeaderField(GEEServerVersionChecker.ENTERPRISE_VERSION_HEADER) ?: return
      GEEServerVersionChecker.checkVersionSupported(versionHeader)
    }

    private fun getErrorText(connection: HttpURLConnection): String? {
      val errorStream = connection.errorStream ?: return null
      val stream = if (connection.contentEncoding == "gzip") GZIPInputStream(errorStream) else errorStream
      return InputStreamReader(stream, Charsets.UTF_8).use { it.readText() }
    }

    private fun getJsonError(connection: HttpURLConnection, errorText: String): GiteeErrorMessage? {
      if (!connection.contentType.startsWith(GiteeApiContentHelper.JSON_MIME_TYPE)) return null
      return try {
        return GiteeApiContentHelper.fromJson(errorText)
      }
      catch (jse: GiteeJsonException) {
        null
      }
    }

    private fun createResponse(request: HttpRequests.Request, indicator: ProgressIndicator): GiteeApiResponse {
      return object : GiteeApiResponse {
        override fun findHeader(headerName: String): String? = request.connection.getHeaderField(headerName)

        override fun <T> readBody(converter: ThrowableConvertor<Reader, T, IOException>): T = request.getReader(indicator).use {
          converter.convert(it)
        }

        override fun <T> handleBody(converter: ThrowableConvertor<InputStream, T, IOException>): T = request.inputStream.use {
          converter.convert(it)
        }
      }
    }
  }

  class Factory {
    fun create(token: String): GiteeApiRequestExecutor = create(token, true)

    fun create(token: String, useProxy: Boolean = true): GiteeApiRequestExecutor = create(useProxy) { token }

    fun create(tokenSupplier: () -> String): GiteeApiRequestExecutor = create(true, tokenSupplier)

    fun create(useProxy: Boolean = true, tokenSupplier: () -> String): GiteeApiRequestExecutor =
      WithTokenAuth(GiteeSettings.getInstance(), tokenSupplier, useProxy)

    fun create(): GiteeApiRequestExecutor = NoAuth(GiteeSettings.getInstance())

    companion object {
      @JvmStatic
      fun getInstance(): Factory = service()
    }
  }

  companion object {
    private val LOG = logger<GiteeApiRequestExecutor>()
  }

  internal class MutableTokenSupplier(token: String) : () -> String {
    private val authDataChangedEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

    @Volatile
    var token: String = token
      set(value) {
        field = value
        runInEdt(ModalityState.any()) {
          authDataChangedEventDispatcher.multicaster.eventOccurred()
        }
      }

    override fun invoke(): String = token

    fun addListener(disposable: Disposable, listener: () -> Unit) =
      SimpleEventListener.addDisposableListener(authDataChangedEventDispatcher, disposable, listener)
  }
}