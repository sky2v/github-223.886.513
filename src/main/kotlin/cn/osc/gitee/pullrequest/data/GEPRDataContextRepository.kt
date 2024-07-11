// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.pullrequest.data

import com.intellij.collaboration.async.disposingScope
import com.intellij.collaboration.ui.icon.AsyncImageIconsProvider
import com.intellij.collaboration.ui.icon.CachingIconsProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.runUnderIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.IconUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.ui.ImageUtil
import git4idea.remote.GitRemoteUrlCoordinates
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import cn.osc.gitee.GiteeIcons
import cn.osc.gitee.api.GEGQLRequests
import cn.osc.gitee.api.GERepositoryCoordinates
import cn.osc.gitee.api.GiteeApiRequestExecutor
import cn.osc.gitee.api.data.GERepositoryOwnerName
import cn.osc.gitee.api.data.GEUser
import cn.osc.gitee.api.util.SimpleGHGQLPagesLoader
import cn.osc.gitee.authentication.accounts.GiteeAccount
import cn.osc.gitee.authentication.accounts.GiteeAccountInformationProvider
import cn.osc.gitee.i18n.GiteeBundle
import cn.osc.gitee.pullrequest.GEPRDiffRequestModelImpl
import cn.osc.gitee.pullrequest.data.service.*
import cn.osc.gitee.util.CachingGHUserAvatarLoader
import cn.osc.gitee.util.GiteeSharedProjectSettings
import java.awt.Image
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon

@Service
internal class GEPRDataContextRepository(private val project: Project) : Disposable {

  private val cs = disposingScope()

  private val cache = ConcurrentHashMap<GERepositoryCoordinates, GEPRDataContext>()
  private val cacheGuard = Mutex()

  suspend fun getContext(repository: GERepositoryCoordinates, remote: GitRemoteUrlCoordinates,
                         account: GiteeAccount, requestExecutor: GiteeApiRequestExecutor): GEPRDataContext =
    withContext(cs.coroutineContext) {
      cacheGuard.withLock {
        val existing = cache[repository]
        if (existing != null) return@withLock existing
        try {
          val context = withContext(Dispatchers.IO) {
            runUnderIndicator {
              loadContext(account, requestExecutor, repository, remote)
            }
          }
          cache[repository] = context
          context
        }
        catch (e: Exception) {
          if (e !is CancellationException) LOG.info("Error occurred while creating data context", e)
          throw e
        }
      }
    }

  suspend fun clearContext(repository: GERepositoryCoordinates) {
    cacheGuard.withLock {
      withContext(cs.coroutineContext) {
        cache.remove(repository)?.let {
          Disposer.dispose(it)
        }
      }
    }
  }

  @RequiresBackgroundThread
  @Throws(IOException::class)
  private fun loadContext(account: GiteeAccount,
                          requestExecutor: GiteeApiRequestExecutor,
                          parsedRepositoryCoordinates: GERepositoryCoordinates,
                          remoteCoordinates: GitRemoteUrlCoordinates): GEPRDataContext {
    val indicator: ProgressIndicator = ProgressManager.getInstance().progressIndicator ?: EmptyProgressIndicator()
    indicator.text = GiteeBundle.message("pull.request.loading.account.info")
    val accountDetails = GiteeAccountInformationProvider.getInstance().getInformation(requestExecutor, indicator, account)
    indicator.checkCanceled()

    indicator.text = GiteeBundle.message("pull.request.loading.repo.info")
    val repositoryInfo =
      requestExecutor.execute(indicator, GEGQLRequests.Repo.find(GERepositoryCoordinates(account.server,
                                                                                         parsedRepositoryCoordinates.repositoryPath)))
      ?: throw IllegalArgumentException(
        "Repository ${parsedRepositoryCoordinates.repositoryPath} does not exist at ${account.server} or you don't have access.")

    val currentUser = GEUser(accountDetails.nodeId, accountDetails.login, accountDetails.htmlUrl, accountDetails.avatarUrl!!,
                             accountDetails.name)

    indicator.text = GiteeBundle.message("pull.request.loading.user.teams.info")
    val repoOwner = repositoryInfo.owner
    val currentUserTeams = if (repoOwner is GERepositoryOwnerName.Organization)
      SimpleGHGQLPagesLoader(requestExecutor, {
        GEGQLRequests.Organization.Team.findByUserLogins(account.server, repoOwner.login, listOf(currentUser.login), it)
      }).loadAll(indicator)
    else emptyList()
    indicator.checkCanceled()

    // repository might have been renamed/moved
    val apiRepositoryPath = repositoryInfo.path
    val apiRepositoryCoordinates = GERepositoryCoordinates(account.server, apiRepositoryPath)

    val securityService = GEPRSecurityServiceImpl(GiteeSharedProjectSettings.getInstance(project),
                                                  account, currentUser, currentUserTeams,
                                                  repositoryInfo)
    val detailsService = GEPRDetailsServiceImpl(ProgressManager.getInstance(), requestExecutor, apiRepositoryCoordinates)
    val stateService = GEPRStateServiceImpl(ProgressManager.getInstance(), securityService,
                                            requestExecutor, account.server, apiRepositoryPath)
    val commentService = GEPRCommentServiceImpl(ProgressManager.getInstance(), requestExecutor, apiRepositoryCoordinates)
    val changesService = GEPRChangesServiceImpl(ProgressManager.getInstance(), project, requestExecutor,
                                                remoteCoordinates, apiRepositoryCoordinates)
    val reviewService = GEPRReviewServiceImpl(ProgressManager.getInstance(), securityService, requestExecutor, apiRepositoryCoordinates)
    val filesService = GEPRFilesServiceImpl(ProgressManager.getInstance(), requestExecutor, apiRepositoryCoordinates)

    val listLoader = GEPRListLoader(ProgressManager.getInstance(), requestExecutor, apiRepositoryCoordinates)
    val listUpdatesChecker = GEPRListETagUpdateChecker(ProgressManager.getInstance(), requestExecutor, account.server, apiRepositoryPath)

    val dataProviderRepository = GEPRDataProviderRepositoryImpl(detailsService, stateService, reviewService, filesService, commentService,
                                                                changesService) { id ->
      GEGQLPagedListLoader(ProgressManager.getInstance(),
                           SimpleGHGQLPagesLoader(requestExecutor, { p ->
                             GEGQLRequests.PullRequest.Timeline.items(account.server, apiRepositoryPath.owner, apiRepositoryPath.repository,
                                                                      id.number, p)
                           }, true))
    }

    val repoDataService = GEPRRepositoryDataServiceImpl(ProgressManager.getInstance(), requestExecutor,
                                                        remoteCoordinates, apiRepositoryCoordinates,
                                                        repoOwner,
                                                        repositoryInfo.id, repositoryInfo.defaultBranch, repositoryInfo.isFork)

    val iconsScope = MainScope()
    val avatarIconsProvider = CachingIconsProvider(AsyncImageIconsProvider(iconsScope, ImageLoader(requestExecutor)))

    val filesManager = GEPRFilesManagerImpl(project, parsedRepositoryCoordinates)

    indicator.checkCanceled()
    val creationService = GEPRCreationServiceImpl(ProgressManager.getInstance(), requestExecutor, repoDataService)
    return GEPRDataContext(listLoader, listUpdatesChecker, dataProviderRepository,
                           securityService, repoDataService, creationService, detailsService, avatarIconsProvider, filesManager,
                           GEPRDiffRequestModelImpl()).also {
      Disposer.register(it, Disposable { iconsScope.cancel() })
    }
  }

  private class ImageLoader(private val requestExecutor: GiteeApiRequestExecutor)
    : AsyncImageIconsProvider.AsyncImageLoader<String> {

    private val avatarsLoader = CachingGHUserAvatarLoader.getInstance()

    override suspend fun load(key: String): Image? =
      avatarsLoader.requestAvatar(requestExecutor, key).await()

    override fun createBaseIcon(key: String?, iconSize: Int): Icon =
      IconUtil.resizeSquared(GiteeIcons.DefaultAvatar, iconSize)

    override suspend fun postProcess(image: Image): Image =
      ImageUtil.createCircleImage(ImageUtil.toBufferedImage(image))
  }

  // dangerous to do this without lock, but making it suspendable is too much work
  fun findContext(repositoryCoordinates: GERepositoryCoordinates): GEPRDataContext? = cache[repositoryCoordinates]

  override fun dispose() {
    runBlocking { cacheGuard.lock() }
    try {
      val toDispose = cache.values.toList()
      cache.clear()
      toDispose.forEach {
        Disposer.dispose(it)
      }
    }
    finally {
      runBlocking { cacheGuard.unlock() }
    }
  }

  companion object {
    private val LOG = logger<GEPRDataContextRepository>()

    fun getInstance(project: Project) = project.service<GEPRDataContextRepository>()
  }
}