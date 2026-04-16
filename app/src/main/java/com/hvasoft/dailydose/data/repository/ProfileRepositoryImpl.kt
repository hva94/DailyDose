package com.hvasoft.dailydose.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import com.hvasoft.dailydose.data.local.FeedAssetStorage
import com.hvasoft.dailydose.data.local.OfflineFeedItemDao
import com.hvasoft.dailydose.data.local.OfflineMediaAssetDao
import com.hvasoft.dailydose.data.local.OfflineMediaAssetEntity
import com.hvasoft.dailydose.data.local.OfflineMediaAssetType
import com.hvasoft.dailydose.data.local.ProfileLocalCache
import com.hvasoft.dailydose.data.network.model.User
import com.hvasoft.dailydose.di.SnapshotsRootStorageQualifier
import com.hvasoft.dailydose.di.UsersDatabaseQualifier
import com.hvasoft.dailydose.domain.model.UserProfile
import com.hvasoft.dailydose.domain.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    @UsersDatabaseQualifier private val usersDatabase: DatabaseReference,
    @SnapshotsRootStorageQualifier private val snapshotsRootStorage: StorageReference,
    private val offlineFeedItemDao: OfflineFeedItemDao,
    private val offlineMediaAssetDao: OfflineMediaAssetDao,
    private val feedAssetStorage: FeedAssetStorage,
    private val profileLocalCache: ProfileLocalCache,
) : ProfileRepository {

    override suspend fun loadUserProfile(userId: String): Result<UserProfile?> =
        withContext(Dispatchers.IO) {
            val cachedOwnerProfile = offlineFeedItemDao.getLatestOwnerProfile(
                accountId = userId,
                ownerUserId = userId,
            )
            val cachedOwnerAvatarLocalPath = offlineFeedItemDao.getLatestOwnerAvatarLocalPath(
                accountId = userId,
                ownerUserId = userId,
            )
            val cachedProfile = profileLocalCache.get(userId)
            val existingAvatarAsset = resolveReusableAvatarAsset(
                userId = userId,
                cachedOwnerAvatarAssetId = cachedOwnerProfile?.ownerAvatarAssetId,
            )
            try {
                val snap = usersDatabase.child(userId).get().await()
                val u = snap.getValue(User::class.java)
                val resolvedDisplayName = u?.userName
                    ?.takeIf(String::isNotBlank)
                    ?: cachedProfile?.displayName
                    ?.takeIf(String::isNotBlank)
                    ?: cachedOwnerProfile?.ownerDisplayName
                    .orEmpty()
                val resolvedPhotoUrl = u?.photoUrl
                    ?.takeIf(String::isNotBlank)
                    ?: cachedProfile?.photoUrl
                    ?.takeIf(String::isNotBlank)
                    ?: cachedOwnerProfile?.ownerAvatarRemoteUrl
                    .orEmpty()
                val resolvedEmail = cachedProfile?.email.orEmpty()
                val retainedAvatarAsset = retainSharedAvatarAsset(
                    userId = userId,
                    remotePhotoUrl = resolvedPhotoUrl,
                    existingAsset = existingAvatarAsset,
                )
                profileLocalCache.save(
                    userId = userId,
                    displayName = resolvedDisplayName,
                    photoUrl = resolvedPhotoUrl,
                    localPhotoPath = retainedAvatarAsset?.localPath
                        ?: cachedOwnerAvatarLocalPath
                        ?: cachedOwnerProfile?.ownerAvatarLocalPath
                            ?.takeIf(String::isNotBlank)
                        ?: existingAvatarAsset?.localPath
                        ?: cachedProfile?.localPhotoPath
                        .orEmpty(),
                    email = resolvedEmail,
                )
                Result.success(
                    UserProfile(
                        userId = userId,
                        displayName = resolvedDisplayName,
                        photoUrl = resolvedPhotoUrl,
                        localPhotoPath = retainedAvatarAsset?.localPath
                            ?: cachedOwnerAvatarLocalPath
                            ?: cachedOwnerProfile?.ownerAvatarLocalPath
                                ?.takeIf(String::isNotBlank)
                            ?: existingAvatarAsset?.localPath
                            ?: cachedProfile?.localPhotoPath
                            .takeIf { it?.isNotBlank() == true },
                        email = resolvedEmail,
                        isOfflineFallback = false,
                    )
                )
            } catch (e: Exception) {
                val fallbackDisplayName = cachedProfile?.displayName
                    ?.takeIf(String::isNotBlank)
                    ?: cachedOwnerProfile?.ownerDisplayName
                    .orEmpty()
                val fallbackPhotoUrl = cachedProfile?.photoUrl
                    ?.takeIf(String::isNotBlank)
                    ?: existingAvatarAsset?.sourceUrl
                    ?.takeIf(String::isNotBlank)
                    ?: cachedOwnerProfile?.ownerAvatarRemoteUrl
                    .orEmpty()
                val fallbackLocalPhotoPath = cachedOwnerAvatarLocalPath
                    ?: cachedOwnerProfile?.ownerAvatarLocalPath
                    ?.takeIf(String::isNotBlank)
                    ?: existingAvatarAsset?.localPath
                    ?: cachedProfile?.localPhotoPath
                        ?.takeIf(String::isNotBlank)
                val fallbackEmail = cachedProfile?.email.orEmpty()
                Result.success(
                    UserProfile(
                        userId = userId,
                        displayName = fallbackDisplayName,
                        photoUrl = fallbackPhotoUrl,
                        localPhotoPath = fallbackLocalPhotoPath,
                        email = fallbackEmail,
                        isOfflineFallback = true,
                    ),
                )
            }
        }

    override suspend fun getCachedAvatarLocalPath(userId: String): String? =
        withContext(Dispatchers.IO) {
            offlineFeedItemDao.getLatestOwnerAvatarLocalPath(
                accountId = userId,
                ownerUserId = userId,
            )?.takeIf { path ->
                File(path).exists()
            } ?: getSharedAvatarAsset(userId)
                ?.localPath
                ?.takeIf { path -> File(path).exists() }
                ?: profileLocalCache.get(userId)
                    ?.localPhotoPath
                    ?.takeIf(String::isNotBlank)
                    ?.takeIf { path -> File(path).exists() }
        }

    override suspend fun uploadProfilePhoto(
        userId: String,
        localImageContentUri: String,
        onProgress: (Int) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(localImageContentUri)
            val ref = snapshotsRootStorage.child(userId).child(PROFILE_IMAGE_FILE_NAME)
            val task = ref.putFile(uri)
            task.addOnProgressListener { taskSnapshot ->
                val total = taskSnapshot.totalByteCount
                onProgress(if (total > 0) (100 * taskSnapshot.bytesTransferred / total).toInt() else 0)
            }
            task.await()
            Result.success(task.snapshot.storage.downloadUrl.await().toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun mergeAndSaveUserRecord(
        userId: String,
        userName: String?,
        photoUrl: String?,
        fallbackDisplayName: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val snap = usersDatabase.child(userId).get().await()
            val current = snap.getValue(User::class.java) ?: User()
            val updated = current.copy(
                userName = userName ?: current.userName.ifEmpty { fallbackDisplayName },
                photoUrl = photoUrl ?: current.photoUrl,
            )
            usersDatabase.child(userId).setValue(updated).await()
            profileLocalCache.save(
                userId = userId,
                displayName = updated.userName,
                photoUrl = updated.photoUrl,
                localPhotoPath = profileLocalCache.get(userId)?.localPhotoPath.orEmpty(),
                email = profileLocalCache.get(userId)?.email.orEmpty(),
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAuthDisplayName(newName: String): Result<Unit> =
        withContext(Dispatchers.Main) {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                    ?: return@withContext Result.failure(IllegalStateException("No signed-in user"))
                user.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(newName).build()
                ).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun retainSharedAvatarAsset(
        userId: String,
        remotePhotoUrl: String,
        existingAsset: OfflineMediaAssetEntity? = null,
    ): OfflineMediaAssetEntity? {
        val assetId = buildCurrentUserAvatarAssetId(userId)
        val resolvedExistingAsset = existingAsset ?: getSharedAvatarAsset(userId)
        if (remotePhotoUrl.isBlank()) {
            return resolvedExistingAsset?.takeIf { asset ->
                asset.localPath?.let(::File)?.exists() == true
            }
        }

        val retainedAsset = feedAssetStorage.retainRemoteAsset(
            accountId = userId,
            assetId = assetId,
            assetType = OfflineMediaAssetType.USER_AVATAR,
            sourceUrl = remotePhotoUrl,
            referencedAt = System.currentTimeMillis(),
            existingAsset = resolvedExistingAsset,
        )
        offlineMediaAssetDao.upsertAll(listOf(retainedAsset))
        return retainedAsset
    }

    private suspend fun getSharedAvatarAsset(userId: String): OfflineMediaAssetEntity? =
        offlineMediaAssetDao.getByIds(listOf(buildCurrentUserAvatarAssetId(userId))).firstOrNull()

    private suspend fun resolveReusableAvatarAsset(
        userId: String,
        cachedOwnerAvatarAssetId: String?,
    ): OfflineMediaAssetEntity? {
        val sharedAvatarAsset = getSharedAvatarAsset(userId)
        if (sharedAvatarAsset?.localPath?.let(::File)?.exists() == true) {
            return sharedAvatarAsset
        }

        val cachedOwnerAvatarAsset = cachedOwnerAvatarAssetId
            ?.takeIf(String::isNotBlank)
            ?.let { assetId -> offlineMediaAssetDao.getByIds(listOf(assetId)).firstOrNull() }

        return cachedOwnerAvatarAsset?.takeIf { asset ->
            asset.localPath?.let(::File)?.exists() == true
        } ?: sharedAvatarAsset ?: cachedOwnerAvatarAsset
    }

    private fun buildCurrentUserAvatarAssetId(userId: String): String = "avatar-$userId-$userId"

    companion object {
        private const val PROFILE_IMAGE_FILE_NAME = "userImageProfile"
    }
}
