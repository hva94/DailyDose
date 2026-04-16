package com.hvasoft.dailydose.data.local

import android.content.Context
import com.hvasoft.dailydose.di.DispatcherIO
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.logging.Logger
import javax.inject.Inject

class FeedAssetStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    @DispatcherIO
    private val dispatcherIO: CoroutineDispatcher,
) {

    suspend fun retainRemoteAsset(
        accountId: String,
        assetId: String,
        assetType: OfflineMediaAssetType,
        sourceUrl: String,
        referencedAt: Long,
        existingAsset: OfflineMediaAssetEntity? = null,
    ): OfflineMediaAssetEntity = withContext(dispatcherIO) {
        if (sourceUrl.isBlank()) {
            existingAsset?.localPath?.let(::File)?.takeIf(File::exists)?.delete()
            return@withContext OfflineMediaAssetEntity(
                assetId = assetId,
                accountId = accountId,
                assetType = assetType,
                sourceUrl = sourceUrl,
                localPath = null,
                downloadStatus = OfflineMediaDownloadStatus.MISSING,
                byteSize = 0L,
                downloadedAt = null,
                lastReferencedAt = referencedAt,
            )
        }

        existingAsset
            ?.takeIf { it.sourceUrl == sourceUrl }
            ?.let(::reuseReadyAssetOrNull)
            ?.let { reusableAsset ->
                return@withContext reusableAsset.copy(lastReferencedAt = referencedAt)
            }

        val accountDir = File(File(context.filesDir, ROOT_DIRECTORY_NAME), accountId).apply {
            mkdirs()
        }
        val assetFile = File(accountDir, buildFileName(assetId, sourceUrl))
        val tempAssetFile = File(accountDir, "${assetFile.name}.download")

        try {
            URL(sourceUrl).openStream().use { input ->
                FileOutputStream(tempAssetFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (assetFile.exists()) {
                assetFile.delete()
            }
            tempAssetFile.copyTo(assetFile, overwrite = true)
            tempAssetFile.delete()

            existingAsset?.localPath
                ?.takeIf { it != assetFile.absolutePath }
                ?.let(::File)
                ?.takeIf(File::exists)
                ?.delete()

            OfflineMediaAssetEntity(
                assetId = assetId,
                accountId = accountId,
                assetType = assetType,
                sourceUrl = sourceUrl,
                localPath = assetFile.absolutePath,
                downloadStatus = OfflineMediaDownloadStatus.READY,
                byteSize = assetFile.length(),
                downloadedAt = referencedAt,
                lastReferencedAt = referencedAt,
            )
        } catch (_: Exception) {
            tempAssetFile.delete()
            logger.warning("Failed to retain offline asset $assetId for account $accountId")
            existingAsset?.let(::reuseReadyAssetOrNull)?.let { reusableAsset ->
                return@withContext reusableAsset.copy(lastReferencedAt = referencedAt)
            }
            OfflineMediaAssetEntity(
                assetId = assetId,
                accountId = accountId,
                assetType = assetType,
                sourceUrl = sourceUrl,
                localPath = null,
                downloadStatus = OfflineMediaDownloadStatus.FAILED,
                byteSize = 0L,
                downloadedAt = null,
                lastReferencedAt = referencedAt,
            )
        }
    }

    suspend fun deleteFiles(paths: List<String>) = withContext(dispatcherIO) {
        paths.forEach { absolutePath ->
            runCatching {
                if (File(absolutePath).delete().not()) {
                    logger.warning("Could not delete offline asset file at $absolutePath")
                }
            }.onFailure {
                logger.warning("Error deleting offline asset file at $absolutePath")
            }
        }
    }

    suspend fun clearAccount(accountId: String) = withContext(dispatcherIO) {
        File(File(context.filesDir, ROOT_DIRECTORY_NAME), accountId).deleteRecursively()
    }

    private fun buildFileName(assetId: String, sourceUrl: String): String {
        val safeAssetId = assetId.replace("[^a-zA-Z0-9-_]".toRegex(), "_")
        val extension = sourceUrl
            .substringAfterLast('/', "")
            .substringAfterLast('.', "img")
            .substringBefore('?')
            .takeIf { it.matches("[a-zA-Z0-9]{1,5}".toRegex()) }
            ?: "img"
        return "$safeAssetId.$extension"
    }

    private fun reuseReadyAssetOrNull(asset: OfflineMediaAssetEntity): OfflineMediaAssetEntity? {
        val localPath = asset.localPath ?: return null
        val localFile = File(localPath)
        if (localFile.exists().not()) return null

        return asset.copy(
            localPath = localFile.absolutePath,
            downloadStatus = OfflineMediaDownloadStatus.READY,
            byteSize = localFile.length(),
        )
    }

    private companion object {
        const val ROOT_DIRECTORY_NAME = "offline_feed_assets"
        val logger: Logger = Logger.getLogger(FeedAssetStorage::class.java.name)
    }
}
