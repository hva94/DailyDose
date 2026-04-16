package com.hvasoft.dailydose.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import com.hvasoft.dailydose.MainDispatcherRule
import com.hvasoft.dailydose.data.local.FeedAssetStorage
import com.hvasoft.dailydose.data.local.OfflineFeedItemEntity
import com.hvasoft.dailydose.data.local.OfflineItemAvailabilityStatus
import com.hvasoft.dailydose.data.local.OfflineMediaAssetType
import com.hvasoft.dailydose.data.local.ProfileLocalCache
import com.hvasoft.dailydose.data.network.model.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.just
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userId = "user-123"
    private lateinit var tempDir: File
    private lateinit var usersDatabase: DatabaseReference
    private lateinit var snapshotsRootStorage: StorageReference
    private lateinit var offlineFeedItemDao: FakeOfflineFeedItemDao
    private lateinit var offlineMediaAssetDao: FakeOfflineMediaAssetDao
    private lateinit var feedAssetStorage: FeedAssetStorage
    private lateinit var profileLocalCache: ProfileLocalCache
    private lateinit var repository: ProfileRepositoryImpl

    @Before
    fun setUp() {
        tempDir = createTempDirectory(prefix = "profile-repository-test").toFile()
        val context = mockk<Context>()
        val sharedPreferences = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>()
        val storedStrings = mutableMapOf<String, String?>()
        every { context.filesDir } returns tempDir
        every {
            context.getSharedPreferences(ProfileLocalCache.PREFS_NAME, Context.MODE_PRIVATE)
        } returns sharedPreferences
        every { sharedPreferences.getString(any(), any()) } answers {
            storedStrings[firstArg<String>()] ?: secondArg()
        }
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } answers {
            storedStrings[firstArg<String>()] = secondArg()
            editor
        }
        every { editor.apply() } just runs

        usersDatabase = mockk(relaxed = true)
        snapshotsRootStorage = mockk(relaxed = true)
        offlineFeedItemDao = FakeOfflineFeedItemDao()
        offlineMediaAssetDao = FakeOfflineMediaAssetDao()
        feedAssetStorage = FeedAssetStorage(context, mainDispatcherRule.dispatcher)
        profileLocalCache = ProfileLocalCache(context)
        repository = ProfileRepositoryImpl(
            usersDatabase = usersDatabase,
            snapshotsRootStorage = snapshotsRootStorage,
            offlineFeedItemDao = offlineFeedItemDao,
            offlineMediaAssetDao = offlineMediaAssetDao,
            feedAssetStorage = feedAssetStorage,
            profileLocalCache = profileLocalCache,
        )
    }

    @Test
    fun `loadUserProfile creates retained local avatar from backend photo url`() = runTest {
        val remotePhotoUrl = newMediaUrl("profile-photo.jpg")
        stubUserRecord(
            User(
                userName = "Henry",
                photoUrl = remotePhotoUrl,
            ),
        )

        val result = repository.loadUserProfile(userId).getOrThrow()

        assertThat(result).isNotNull()
        assertThat(result?.photoUrl).isEqualTo(remotePhotoUrl)
        assertThat(result?.localPhotoPath).isNotNull()
        assertThat(File(result?.localPhotoPath.orEmpty()).exists()).isTrue()
        assertThat(result?.isOfflineFallback).isFalse()
        assertThat(
            offlineMediaAssetDao.storedAssets(userId).map { it.assetId },
        ).contains("avatar-$userId-$userId")
    }

    @Test
    fun `loadUserProfile keeps previous retained avatar when new remote photo cannot be retained`() = runTest {
        val previousPhotoUrl = newMediaUrl("profile-photo-old.jpg")
        val previousAsset = feedAssetStorage.retainRemoteAsset(
            accountId = userId,
            assetId = "avatar-$userId-$userId",
            assetType = OfflineMediaAssetType.USER_AVATAR,
            sourceUrl = previousPhotoUrl,
            referencedAt = 123L,
        )
        offlineMediaAssetDao.upsertAll(listOf(previousAsset))
        stubUserRecord(
            User(
                userName = "Henry",
                photoUrl = "bad://new-profile-photo.jpg",
            ),
        )

        val result = repository.loadUserProfile(userId).getOrThrow()

        assertThat(result).isNotNull()
        assertThat(result?.photoUrl).isEqualTo("bad://new-profile-photo.jpg")
        assertThat(result?.localPhotoPath).isEqualTo(previousAsset.localPath)
        assertThat(File(result?.localPhotoPath.orEmpty()).exists()).isTrue()
        assertThat(result?.isOfflineFallback).isFalse()
    }

    @Test
    fun `loadUserProfile reuses retained avatar when backend photo url is blank`() = runTest {
        val previousPhotoUrl = newMediaUrl("profile-photo-retained.jpg")
        val previousAsset = feedAssetStorage.retainRemoteAsset(
            accountId = userId,
            assetId = "avatar-$userId-$userId",
            assetType = OfflineMediaAssetType.USER_AVATAR,
            sourceUrl = previousPhotoUrl,
            referencedAt = 123L,
        )
        offlineMediaAssetDao.upsertAll(listOf(previousAsset))
        stubUserRecord(
            User(
                userName = "Henry",
                photoUrl = "",
            ),
        )

        val result = repository.loadUserProfile(userId).getOrThrow()

        assertThat(result).isNotNull()
        assertThat(result?.localPhotoPath).isEqualTo(previousAsset.localPath)
        assertThat(File(result?.localPhotoPath.orEmpty()).exists()).isTrue()
        assertThat(result?.photoUrl).isEmpty()
    }

    @Test
    fun `loadUserProfile falls back to cached avatar when backend read fails`() = runTest {
        val previousPhotoUrl = newMediaUrl("profile-photo-cached.jpg")
        val previousAsset = feedAssetStorage.retainRemoteAsset(
            accountId = userId,
            assetId = "avatar-$userId-$userId",
            assetType = OfflineMediaAssetType.USER_AVATAR,
            sourceUrl = previousPhotoUrl,
            referencedAt = 123L,
        )
        offlineMediaAssetDao.upsertAll(listOf(previousAsset))
        val userReference = mockk<DatabaseReference>()
        every { usersDatabase.child(userId) } returns userReference
        every { userReference.get() } returns Tasks.forException(IllegalStateException("offline"))

        val result = repository.loadUserProfile(userId).getOrThrow()

        assertThat(result).isNotNull()
        assertThat(result?.displayName).isEmpty()
        assertThat(result?.photoUrl).isEqualTo(previousPhotoUrl)
        assertThat(result?.localPhotoPath).isEqualTo(previousAsset.localPath)
        assertThat(result?.isOfflineFallback).isTrue()
    }

    @Test
    fun `loadUserProfile falls back to local profile cache when backend read fails and feed cache is empty`() = runTest {
        val cachedLocalPhoto = File(tempDir, "profile-local-cached.jpg").apply {
            writeText("avatar")
        }
        profileLocalCache.save(
            userId = userId,
            displayName = "Henry Local",
            photoUrl = "https://example.com/local-photo.jpg",
            localPhotoPath = cachedLocalPhoto.absolutePath,
            email = "henry.local@example.com",
        )
        val userReference = mockk<DatabaseReference>()
        every { usersDatabase.child(userId) } returns userReference
        every { userReference.get() } returns Tasks.forException(IllegalStateException("offline"))

        val result = repository.loadUserProfile(userId).getOrThrow()

        assertThat(result).isNotNull()
        assertThat(result?.displayName).isEqualTo("Henry Local")
        assertThat(result?.photoUrl).isEqualTo("https://example.com/local-photo.jpg")
        assertThat(result?.localPhotoPath).isEqualTo(cachedLocalPhoto.absolutePath)
        assertThat(result?.email).isEqualTo("henry.local@example.com")
        assertThat(result?.isOfflineFallback).isTrue()
    }

    @Test
    fun `loadUserProfile falls back to cached owner name and avatar when backend read fails`() = runTest {
        val previousPhotoUrl = newMediaUrl("profile-photo-cached-name.jpg")
        val previousAsset = feedAssetStorage.retainRemoteAsset(
            accountId = userId,
            assetId = "avatar-$userId-$userId",
            assetType = OfflineMediaAssetType.USER_AVATAR,
            sourceUrl = previousPhotoUrl,
            referencedAt = 123L,
        )
        offlineMediaAssetDao.upsertAll(listOf(previousAsset))
        offlineFeedItemDao.upsertAll(
            listOf(
                OfflineFeedItemEntity(
                    accountId = userId,
                    snapshotId = "snapshot-1",
                    ownerUserId = userId,
                    title = "Mine",
                    publishedAt = 100L,
                    sortOrder = 0L,
                    remotePhotoUrl = "https://example.com/post.jpg",
                    mainImageAssetId = null,
                    ownerDisplayName = "Henry Cached",
                    ownerAvatarRemoteUrl = previousPhotoUrl,
                    ownerAvatarAssetId = "avatar-$userId-$userId",
                    likeCount = 0,
                    likedByCurrentUser = false,
                    availabilityStatus = OfflineItemAvailabilityStatus.MEDIA_PARTIAL,
                    syncedAt = 200L,
                ),
            ),
        )
        val userReference = mockk<DatabaseReference>()
        every { usersDatabase.child(userId) } returns userReference
        every { userReference.get() } returns Tasks.forException(IllegalStateException("offline"))

        val result = repository.loadUserProfile(userId).getOrThrow()

        assertThat(result).isNotNull()
        assertThat(result?.displayName).isEqualTo("Henry Cached")
        assertThat(result?.photoUrl).isEqualTo(previousPhotoUrl)
        assertThat(result?.localPhotoPath).isEqualTo(previousAsset.localPath)
        assertThat(result?.isOfflineFallback).isTrue()
    }

    @Test
    fun `loadUserProfile reuses owner avatar local path from offline feed cache`() = runTest {
        val previousPhotoUrl = newMediaUrl("profile-photo-owner-local.jpg")
        val previousAsset = feedAssetStorage.retainRemoteAsset(
            accountId = userId,
            assetId = "avatar-$userId-$userId",
            assetType = OfflineMediaAssetType.USER_AVATAR,
            sourceUrl = previousPhotoUrl,
            referencedAt = 123L,
        )
        offlineMediaAssetDao.upsertAll(listOf(previousAsset))
        offlineFeedItemDao.upsertAll(
            listOf(
                OfflineFeedItemEntity(
                    accountId = userId,
                    snapshotId = "snapshot-owner-local",
                    ownerUserId = userId,
                    title = "Mine",
                    publishedAt = 100L,
                    sortOrder = 0L,
                    remotePhotoUrl = "https://example.com/post.jpg",
                    mainImageAssetId = null,
                    ownerDisplayName = "Henry Cached",
                    ownerAvatarRemoteUrl = previousPhotoUrl,
                    ownerAvatarAssetId = "avatar-$userId-$userId",
                    likeCount = 0,
                    likedByCurrentUser = false,
                    availabilityStatus = OfflineItemAvailabilityStatus.MEDIA_PARTIAL,
                    syncedAt = 200L,
                ),
            ),
        )
        stubUserRecord(
            User(
                userName = "Henry",
                photoUrl = "",
            ),
        )

        val result = repository.loadUserProfile(userId).getOrThrow()

        assertThat(result).isNotNull()
        assertThat(result?.localPhotoPath).isEqualTo(previousAsset.localPath)
        assertThat(File(result?.localPhotoPath.orEmpty()).exists()).isTrue()
    }

    private fun stubUserRecord(user: User) {
        val userReference = mockk<DatabaseReference>()
        val snapshot = mockk<DataSnapshot>()
        every { usersDatabase.child(userId) } returns userReference
        every { userReference.get() } returns Tasks.forResult(snapshot)
        every { snapshot.getValue(User::class.java) } returns user
    }

    private fun newMediaUrl(fileName: String): String {
        val file = File(tempDir, fileName).apply {
            writeText(fileName)
        }
        return file.toURI().toURL().toString()
    }
}
