package com.hvasoft.dailydose.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.hvasoft.dailydose.MainDispatcherRule
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.data.local.FeedAssetStorage
import com.hvasoft.dailydose.data.local.HomeFeedTransactionRunner
import com.hvasoft.dailydose.data.local.OfflineFeedItemEntity
import com.hvasoft.dailydose.data.local.OfflineItemAvailabilityStatus
import com.hvasoft.dailydose.data.local.OfflineMediaAssetEntity
import com.hvasoft.dailydose.data.local.OfflineMediaAssetType
import com.hvasoft.dailydose.data.local.ProfileLocalCache
import com.hvasoft.dailydose.domain.model.HomeFeedLastRefreshResult
import com.hvasoft.dailydose.domain.model.Snapshot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

@OptIn(ExperimentalCoroutinesApi::class)
class HomeFeedRefreshCoordinatorTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountId = "user-123"
    private lateinit var tempDir: File
    private lateinit var assetStorage: FeedAssetStorage
    private lateinit var offlineFeedItemDao: FakeOfflineFeedItemDao
    private lateinit var offlineMediaAssetDao: FakeOfflineMediaAssetDao
    private lateinit var feedSyncStateDao: FakeFeedSyncStateDao
    private lateinit var profileLocalCache: ProfileLocalCache

    @Before
    fun setUp() {
        tempDir = createTempDirectory(prefix = "offline-feed-test").toFile()
        val context = mockk<Context>()
        val sharedPreferences = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>()
        val storedStrings = mutableMapOf<String, String?>()
        every { context.filesDir } returns tempDir
        every {
            context.getSharedPreferences(ProfileLocalCache.PREFS_NAME, Context.MODE_PRIVATE)
        } returns sharedPreferences
        every { sharedPreferences.getString(any(), any()) } answers {
            storedStrings[invocation.args[0] as String] ?: invocation.args[1] as String?
        }
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } answers {
            storedStrings[invocation.args[0] as String] = invocation.args[1] as String?
            editor
        }
        every { editor.apply() } just runs

        assetStorage = FeedAssetStorage(context, mainDispatcherRule.dispatcher)
        offlineFeedItemDao = FakeOfflineFeedItemDao()
        offlineMediaAssetDao = FakeOfflineMediaAssetDao()
        feedSyncStateDao = FakeFeedSyncStateDao()
        profileLocalCache = ProfileLocalCache(context)
    }

    @Test
    fun `refresh retains latest fifty feed items and marks sync successful`() = runTest {
        val mediaUrl = newMediaUrl("seed-image.jpg")
        val snapshots = (0 until 60).map { index ->
            snapshot(
                snapshotId = "snapshot-$index",
                photoUrl = mediaUrl,
                ownerUserId = "owner-$index",
                title = "Snapshot $index",
                publishedAt = 1_000L - index,
                likeCount = index,
            )
        }
        val coordinator = createCoordinator(
            snapshots = snapshots,
            namesByUserId = snapshots.associate { it.idUserOwner.orEmpty() to "User ${it.snapshotKey}" },
            avatarsByUserId = snapshots.associate { it.idUserOwner.orEmpty() to mediaUrl },
        )

        val result = coordinator.refresh(accountId)

        assertThat(result.isSuccess).isTrue()
        assertThat(offlineFeedItemDao.storedItems(accountId)).hasSize(Constants.OFFLINE_FEED_RETENTION_LIMIT)
        assertThat(feedSyncStateDao.currentState(accountId)?.lastRefreshResult)
            .isEqualTo(HomeFeedLastRefreshResult.SUCCESS)
        assertThat(offlineMediaAssetDao.storedAssets(accountId)).isNotEmpty()
    }

    @Test
    fun `refresh skips retained row and asset rewrites when metadata and files are unchanged`() = runTest {
        val mediaUrl = newMediaUrl("snapshot-unchanged.jpg")
        val avatarUrl = newMediaUrl("avatar-unchanged.jpg")
        val snapshot = snapshot(
            snapshotId = "snapshot-1",
            photoUrl = mediaUrl,
            ownerUserId = "owner-1",
            title = "Already cached",
            publishedAt = 100L,
        )
        seedRetainedSnapshot(
            snapshot = snapshot,
            ownerDisplayName = "Owner 1",
            avatarUrl = avatarUrl,
            referencedAt = 500L,
        )

        val existingItem = offlineFeedItemDao.storedItems(accountId).single()
        val existingMainAsset = offlineMediaAssetDao.storedAssets(accountId)
            .first { it.assetId == snapshotAssetId(snapshot.snapshotKey) }

        val coordinator = createCoordinator(
            snapshots = listOf(snapshot),
            namesByUserId = mapOf("owner-1" to "Owner 1"),
            avatarsByUserId = mapOf("owner-1" to avatarUrl),
        )

        val result = coordinator.refresh(accountId)

        assertThat(result.isSuccess).isTrue()
        assertThat(offlineFeedItemDao.storedItems(accountId).single().syncedAt).isEqualTo(existingItem.syncedAt)
        assertThat(
            offlineMediaAssetDao.storedAssets(accountId)
                .first { it.assetId == snapshotAssetId(snapshot.snapshotKey) }
                .downloadedAt,
        ).isEqualTo(existingMainAsset.downloadedAt)
        assertThat(feedSyncStateDao.currentState(accountId)?.lastSuccessfulSyncAt).isNotNull()
        assertThat(feedSyncStateDao.currentState(accountId)?.lastSuccessfulSyncAt!!).isGreaterThan(500L)
    }

    @Test
    fun `refresh downloads only the changed snapshot image asset`() = runTest {
        val originalPhotoUrl = newMediaUrl("snapshot-original.jpg")
        val updatedPhotoUrl = newMediaUrl("snapshot-updated.jpg")
        val unchangedPhotoUrl = newMediaUrl("snapshot-stable.jpg")
        val firstSnapshot = snapshot(
            snapshotId = "snapshot-1",
            photoUrl = originalPhotoUrl,
            ownerUserId = "owner-1",
            publishedAt = 200L,
        )
        val secondSnapshot = snapshot(
            snapshotId = "snapshot-2",
            photoUrl = unchangedPhotoUrl,
            ownerUserId = "owner-2",
            publishedAt = 100L,
        )
        seedRetainedSnapshot(firstSnapshot, ownerDisplayName = "Owner 1", referencedAt = 700L)
        seedRetainedSnapshot(secondSnapshot, ownerDisplayName = "Owner 2", referencedAt = 700L)
        val unchangedDownloadedAt = offlineMediaAssetDao.storedAssets(accountId)
            .first { it.assetId == snapshotAssetId(secondSnapshot.snapshotKey) }
            .downloadedAt

        val coordinator = createCoordinator(
            snapshots = listOf(
                firstSnapshot.copy(photoUrl = updatedPhotoUrl),
                secondSnapshot,
            ),
            namesByUserId = mapOf(
                "owner-1" to "Owner 1",
                "owner-2" to "Owner 2",
            ),
        )

        val result = coordinator.refresh(accountId)

        assertThat(result.isSuccess).isTrue()
        val assetsById = offlineMediaAssetDao.storedAssets(accountId).associateBy(OfflineMediaAssetEntity::assetId)
        assertThat(assetsById.getValue(snapshotAssetId(firstSnapshot.snapshotKey)).sourceUrl)
            .isEqualTo(updatedPhotoUrl)
        assertThat(assetsById.getValue(snapshotAssetId(secondSnapshot.snapshotKey)).downloadedAt)
            .isEqualTo(unchangedDownloadedAt)
    }

    @Test
    fun `refresh updates only the changed avatar asset`() = runTest {
        val photoUrl = newMediaUrl("snapshot-avatar-test.jpg")
        val originalAvatarUrl = newMediaUrl("avatar-original.jpg")
        val updatedAvatarUrl = newMediaUrl("avatar-updated.jpg")
        val snapshot = snapshot(
            snapshotId = "snapshot-1",
            photoUrl = photoUrl,
            ownerUserId = "owner-1",
            publishedAt = 100L,
        )
        seedRetainedSnapshot(
            snapshot = snapshot,
            ownerDisplayName = "Owner 1",
            avatarUrl = originalAvatarUrl,
            referencedAt = 700L,
        )
        val originalMainAsset = offlineMediaAssetDao.storedAssets(accountId)
            .first { it.assetId == snapshotAssetId(snapshot.snapshotKey) }

        val coordinator = createCoordinator(
            snapshots = listOf(snapshot),
            namesByUserId = mapOf("owner-1" to "Owner 1"),
            avatarsByUserId = mapOf("owner-1" to updatedAvatarUrl),
        )

        val result = coordinator.refresh(accountId)

        assertThat(result.isSuccess).isTrue()
        val assetsById = offlineMediaAssetDao.storedAssets(accountId).associateBy(OfflineMediaAssetEntity::assetId)
        assertThat(assetsById.getValue(avatarAssetId("owner-1")).sourceUrl).isEqualTo(updatedAvatarUrl)
        assertThat(assetsById.getValue(snapshotAssetId(snapshot.snapshotKey)).downloadedAt)
            .isEqualTo(originalMainAsset.downloadedAt)
    }

    @Test
    fun `refresh re-downloads an unchanged asset when the retained file is missing`() = runTest {
        val photoUrl = newMediaUrl("snapshot-missing-file.jpg")
        val snapshot = snapshot(
            snapshotId = "snapshot-1",
            photoUrl = photoUrl,
            ownerUserId = "owner-1",
            publishedAt = 100L,
        )
        seedRetainedSnapshot(snapshot, ownerDisplayName = "Owner 1", referencedAt = 400L)
        val storedAssetBeforeRefresh = offlineMediaAssetDao.storedAssets(accountId)
            .first { it.assetId == snapshotAssetId(snapshot.snapshotKey) }
        File(storedAssetBeforeRefresh.localPath.orEmpty()).delete()

        val coordinator = createCoordinator(
            snapshots = listOf(snapshot),
            namesByUserId = mapOf("owner-1" to "Owner 1"),
        )

        val result = coordinator.refresh(accountId)

        assertThat(result.isSuccess).isTrue()
        val refreshedAsset = offlineMediaAssetDao.storedAssets(accountId)
            .first { it.assetId == snapshotAssetId(snapshot.snapshotKey) }
        assertThat(refreshedAsset.downloadedAt).isNotNull()
        assertThat(storedAssetBeforeRefresh.downloadedAt).isNotNull()
        assertThat(refreshedAsset.downloadedAt!!).isGreaterThan(storedAssetBeforeRefresh.downloadedAt!!)
        assertThat(File(refreshedAsset.localPath.orEmpty()).exists()).isTrue()
    }

    @Test
    fun `refresh prunes removed posts and deletes stale retained files`() = runTest {
        val firstPhotoUrl = newMediaUrl("snapshot-first.jpg")
        val secondPhotoUrl = newMediaUrl("snapshot-second.jpg")
        val firstSnapshot = snapshot(
            snapshotId = "snapshot-1",
            photoUrl = firstPhotoUrl,
            ownerUserId = "owner-1",
            publishedAt = 200L,
        )
        val secondSnapshot = snapshot(
            snapshotId = "snapshot-2",
            photoUrl = secondPhotoUrl,
            ownerUserId = "owner-2",
            publishedAt = 100L,
        )
        seedRetainedSnapshot(firstSnapshot, ownerDisplayName = "Owner 1", referencedAt = 400L)
        seedRetainedSnapshot(secondSnapshot, ownerDisplayName = "Owner 2", referencedAt = 400L)
        val staleAssetPath = offlineMediaAssetDao.storedAssets(accountId)
            .first { it.assetId == snapshotAssetId(secondSnapshot.snapshotKey) }
            .localPath

        val coordinator = createCoordinator(
            snapshots = listOf(firstSnapshot),
            namesByUserId = mapOf("owner-1" to "Owner 1"),
        )

        val result = coordinator.refresh(accountId)

        assertThat(result.isSuccess).isTrue()
        assertThat(offlineFeedItemDao.storedItems(accountId).map(OfflineFeedItemEntity::snapshotId))
            .containsExactly(firstSnapshot.snapshotKey)
        assertThat(offlineMediaAssetDao.storedAssets(accountId).map(OfflineMediaAssetEntity::assetId))
            .containsExactly(snapshotAssetId(firstSnapshot.snapshotKey))
        assertThat(staleAssetPath).isNotNull()
        assertThat(File(staleAssetPath!!).exists()).isFalse()
    }

    private fun createCoordinator(
        snapshots: List<Snapshot>,
        namesByUserId: Map<String, String> = emptyMap(),
        avatarsByUserId: Map<String, String> = emptyMap(),
    ): HomeFeedRefreshCoordinator = HomeFeedRefreshCoordinator(
        remoteDatabaseService = FakeRemoteDatabaseService(
            snapshots = snapshots,
            namesByUserId = namesByUserId,
            avatarsByUserId = avatarsByUserId,
        ),
        transactionRunner = { block -> block() },
        offlineFeedItemDao = offlineFeedItemDao,
        offlineMediaAssetDao = offlineMediaAssetDao,
        feedSyncStateDao = feedSyncStateDao,
        feedAssetStorage = assetStorage,
        profileLocalCache = profileLocalCache,
        dispatcherIO = mainDispatcherRule.dispatcher,
    )

    private suspend fun seedRetainedSnapshot(
        snapshot: Snapshot,
        ownerDisplayName: String,
        avatarUrl: String = "",
        referencedAt: Long,
    ) {
        val mainAsset = assetStorage.retainRemoteAsset(
            accountId = accountId,
            assetId = snapshotAssetId(snapshot.snapshotKey),
            assetType = OfflineMediaAssetType.SNAPSHOT_IMAGE,
            sourceUrl = snapshot.photoUrl.orEmpty(),
            referencedAt = referencedAt,
        )
        val avatarAsset = avatarUrl.takeIf(String::isNotBlank)?.let {
            assetStorage.retainRemoteAsset(
                accountId = accountId,
                assetId = avatarAssetId(snapshot.idUserOwner.orEmpty()),
                assetType = OfflineMediaAssetType.USER_AVATAR,
                sourceUrl = avatarUrl,
                referencedAt = referencedAt,
            )
        }
        offlineMediaAssetDao.upsertAll(listOfNotNull(mainAsset, avatarAsset))
        offlineFeedItemDao.upsertAll(
            listOf(
                OfflineFeedItemEntity(
                    accountId = accountId,
                    snapshotId = snapshot.snapshotKey,
                    ownerUserId = snapshot.idUserOwner.orEmpty(),
                    title = snapshot.title.orEmpty(),
                    publishedAt = snapshot.dateTime ?: referencedAt,
                    sortOrder = 0L,
                    remotePhotoUrl = snapshot.photoUrl.orEmpty(),
                    mainImageAssetId = mainAsset.assetId,
                    ownerDisplayName = ownerDisplayName,
                    ownerAvatarRemoteUrl = avatarUrl,
                    ownerAvatarAssetId = avatarAsset?.assetId,
                    likeCount = snapshot.likeCount.toIntOrNull() ?: 0,
                    likedByCurrentUser = false,
                    availabilityStatus = if (mainAsset.localPath != null) {
                        OfflineItemAvailabilityStatus.FULLY_AVAILABLE
                    } else {
                        OfflineItemAvailabilityStatus.MEDIA_PARTIAL
                    },
                    syncedAt = referencedAt,
                ),
            ),
        )
    }

    private fun snapshot(
        snapshotId: String,
        photoUrl: String,
        ownerUserId: String,
        title: String = snapshotId,
        publishedAt: Long,
        likeCount: Int = 0,
    ): Snapshot = Snapshot(
        snapshotKey = snapshotId,
        photoUrl = photoUrl,
        idUserOwner = ownerUserId,
        title = title,
        dateTime = publishedAt,
        likeCount = likeCount.toString(),
    )

    private fun snapshotAssetId(snapshotId: String): String = "snapshot-$accountId-$snapshotId"

    private fun avatarAssetId(ownerUserId: String): String = "avatar-$accountId-$ownerUserId"

    private fun newMediaUrl(fileName: String): String {
        val sourceFile = File(tempDir, fileName).apply {
            writeText(fileName)
        }
        return sourceFile.toURI().toURL().toString()
    }
}
