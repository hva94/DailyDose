package com.hvasoft.dailydose.data.repository

import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.hvasoft.dailydose.MainDispatcherRule
import com.hvasoft.dailydose.data.auth.FakeAuthSessionProvider
import com.hvasoft.dailydose.data.local.FeedAssetStorage
import com.hvasoft.dailydose.data.local.FeedSyncStateEntity
import com.hvasoft.dailydose.data.local.OfflineFeedItemEntity
import com.hvasoft.dailydose.data.local.OfflineFeedItemWithAssets
import com.hvasoft.dailydose.data.local.OfflineFeedMapper
import com.hvasoft.dailydose.data.local.OfflineItemAvailabilityStatus
import com.hvasoft.dailydose.data.local.OfflineMediaAssetEntity
import com.hvasoft.dailydose.data.local.OfflineMediaDownloadStatus
import com.hvasoft.dailydose.data.local.OfflineMediaAssetType
import com.hvasoft.dailydose.domain.model.HomeFeedAvailabilityMode
import com.hvasoft.dailydose.domain.model.HomeFeedLastRefreshResult
import com.hvasoft.dailydose.domain.model.Snapshot
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.every
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountId = "user-123"
    private lateinit var offlineFeedItemDao: FakeOfflineFeedItemDao
    private lateinit var offlineMediaAssetDao: FakeOfflineMediaAssetDao
    private lateinit var feedSyncStateDao: FakeFeedSyncStateDao
    private lateinit var remoteDatabaseService: com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseService
    private lateinit var repository: HomeRepositoryImpl
    private lateinit var authSessionProvider: FakeAuthSessionProvider

    @Before
    fun setUp() {
        val currentUser = mockk<FirebaseUser>()
        every { currentUser.uid } returns accountId
        every { currentUser.displayName } returns "Henry"
        every { currentUser.email } returns "henry@example.com"
        every { currentUser.photoUrl } returns null
        authSessionProvider = FakeAuthSessionProvider(currentUser)

        offlineFeedItemDao = FakeOfflineFeedItemDao()
        offlineMediaAssetDao = FakeOfflineMediaAssetDao()
        feedSyncStateDao = FakeFeedSyncStateDao()
        remoteDatabaseService = mockk(relaxed = true)
        repository = HomeRepositoryImpl(
            remoteDatabaseService = remoteDatabaseService,
            offlineFeedItemDao = offlineFeedItemDao,
            offlineMediaAssetDao = offlineMediaAssetDao,
            feedSyncStateDao = feedSyncStateDao,
            offlineFeedMapper = OfflineFeedMapper(),
            refreshCoordinator = mockk(relaxed = true),
            authSessionProvider = authSessionProvider,
            feedAssetStorage = mockk<FeedAssetStorage>(relaxed = true),
        )
    }

    @Test
    fun `getPagedSnapshots returns a flow backed by the retained local source`() = runTest {
        offlineFeedItemDao.setPagingItems(
            accountId = accountId,
            items = listOf(
                retainedItem(snapshotId = "snapshot-b", title = "Second item", sortOrder = 0L),
                retainedItem(snapshotId = "snapshot-a", title = "First item", sortOrder = 1L),
            ),
        )

        val snapshotsFlow = repository.getPagedSnapshots()

        assertThat(snapshotsFlow).isNotNull()
    }

    @Test
    fun `observeSyncState maps retained failed refresh to offline retained mode`() = runTest {
        feedSyncStateDao.setState(
            FeedSyncStateEntity(
                accountId = accountId,
                lastSuccessfulSyncAt = 123L,
                lastRefreshAttemptAt = 456L,
                lastRefreshResult = HomeFeedLastRefreshResult.NETWORK_FAILURE,
                retainedItemCount = 2,
                retentionLimit = 50,
                hasRetainedContent = true,
            ),
        )

        val state = repository.observeSyncState().first()

        assertThat(state.availabilityMode).isEqualTo(HomeFeedAvailabilityMode.OFFLINE_RETAINED)
        assertThat(state.hasRetainedContent).isTrue()
        assertThat(state.lastSuccessfulSyncAt).isEqualTo(123L)
    }

    @Test
    fun `observeSyncState returns default state when signed out`() = runTest {
        authSessionProvider.setCurrentUser(null)

        val state = repository.observeSyncState().first()

        assertThat(state.hasRetainedContent).isFalse()
        assertThat(state.retainedItemCount).isEqualTo(0)
    }

    @Test
    fun `cachePostedSnapshot inserts the created snapshot at the top of the local cache and reuses the retained avatar`() = runTest {
        val retainedAvatarAssetId = "avatar-$accountId-$accountId"
        offlineFeedItemDao.upsertAll(
            listOf(
                OfflineFeedItemEntity(
                    accountId = accountId,
                    snapshotId = "older",
                    ownerUserId = accountId,
                    title = "Older",
                    publishedAt = 100L,
                    sortOrder = 0L,
                    remotePhotoUrl = "https://example.com/older.jpg",
                    mainImageAssetId = null,
                    ownerDisplayName = "Henry",
                    ownerAvatarRemoteUrl = "",
                    ownerAvatarAssetId = null,
                    likeCount = 0,
                    likedByCurrentUser = false,
                    availabilityStatus = OfflineItemAvailabilityStatus.MEDIA_PARTIAL,
                    syncedAt = 100L,
                ),
            ),
        )
        offlineMediaAssetDao.upsertAll(
            listOf(
                OfflineMediaAssetEntity(
                    assetId = retainedAvatarAssetId,
                    accountId = accountId,
                    assetType = OfflineMediaAssetType.USER_AVATAR,
                    sourceUrl = "https://cached.example.com/avatar.jpg",
                    localPath = "/tmp/avatar.jpg",
                    downloadStatus = OfflineMediaDownloadStatus.READY,
                    byteSize = 12L,
                    downloadedAt = 50L,
                    lastReferencedAt = 100L,
                ),
            ),
        )
        coEvery { remoteDatabaseService.getUserPhotoUrlOnce(accountId) } returns "https://backend.example.com/avatar.jpg"

        repository.cachePostedSnapshot(
            Snapshot(
                title = "Newest",
                dateTime = 200L,
                photoUrl = "https://example.com/newest.jpg",
                snapshotKey = "newest",
                idUserOwner = accountId,
                userName = "Henry",
            ),
        )

        val cachedItems = offlineFeedItemDao.getByAccount(accountId)
        assertThat(cachedItems.map(OfflineFeedItemEntity::snapshotId)).containsExactly("newest", "older").inOrder()
        assertThat(cachedItems.map(OfflineFeedItemEntity::sortOrder)).containsExactly(0L, 1L).inOrder()
        assertThat(cachedItems.first().ownerAvatarRemoteUrl).isEqualTo("https://backend.example.com/avatar.jpg")
        assertThat(cachedItems.first().ownerAvatarAssetId).isEqualTo(retainedAvatarAssetId)
        assertThat(feedSyncStateDao.currentState(accountId)?.lastRefreshResult)
            .isEqualTo(HomeFeedLastRefreshResult.SUCCESS)
    }

    @Test
    fun `toggleUserLike reverts the local cache when the remote update fails`() = runTest {
        offlineFeedItemDao.upsertAll(
            listOf(
                OfflineFeedItemEntity(
                    accountId = accountId,
                    snapshotId = "snapshot-1",
                    ownerUserId = accountId,
                    title = "Snapshot",
                    publishedAt = 100L,
                    sortOrder = 0L,
                    remotePhotoUrl = "https://example.com/snapshot.jpg",
                    mainImageAssetId = null,
                    ownerDisplayName = "Henry",
                    ownerAvatarRemoteUrl = "",
                    ownerAvatarAssetId = null,
                    likeCount = 2,
                    likedByCurrentUser = false,
                    availabilityStatus = OfflineItemAvailabilityStatus.MEDIA_PARTIAL,
                    syncedAt = 100L,
                ),
            ),
        )
        coEvery {
            remoteDatabaseService.toggleUserLike(any(), any())
        } throws IllegalStateException("network")

        runCatching {
            repository.toggleUserLike(
                Snapshot(
                    snapshotKey = "snapshot-1",
                    likeCount = "2",
                    isLikedByCurrentUser = false,
                ),
                isChecked = true,
            )
        }

        val cachedSnapshot = offlineFeedItemDao.getBySnapshotId(accountId, "snapshot-1")
        assertThat(cachedSnapshot?.likeCount).isEqualTo(2)
        assertThat(cachedSnapshot?.likedByCurrentUser).isFalse()
    }

    @Test
    fun `deleteSnapshot removes the cached item without delegating to refresh`() = runTest {
        offlineFeedItemDao.upsertAll(
            listOf(
                OfflineFeedItemEntity(
                    accountId = accountId,
                    snapshotId = "snapshot-1",
                    ownerUserId = accountId,
                    title = "Snapshot",
                    publishedAt = 100L,
                    sortOrder = 0L,
                    remotePhotoUrl = "https://example.com/snapshot.jpg",
                    mainImageAssetId = "asset-1",
                    ownerDisplayName = "Henry",
                    ownerAvatarRemoteUrl = "",
                    ownerAvatarAssetId = "avatar-1",
                    likeCount = 2,
                    likedByCurrentUser = false,
                    availabilityStatus = OfflineItemAvailabilityStatus.MEDIA_PARTIAL,
                    syncedAt = 100L,
                ),
            ),
        )
        offlineMediaAssetDao.upsertAll(
            listOf(
                OfflineMediaAssetEntity(
                    assetId = "asset-1",
                    accountId = accountId,
                    assetType = OfflineMediaAssetType.SNAPSHOT_IMAGE,
                    sourceUrl = "https://example.com/snapshot.jpg",
                    localPath = null,
                    downloadStatus = OfflineMediaDownloadStatus.MISSING,
                    byteSize = 0L,
                    downloadedAt = null,
                    lastReferencedAt = 100L,
                ),
                OfflineMediaAssetEntity(
                    assetId = "avatar-1",
                    accountId = accountId,
                    assetType = OfflineMediaAssetType.USER_AVATAR,
                    sourceUrl = "https://example.com/avatar.jpg",
                    localPath = null,
                    downloadStatus = OfflineMediaDownloadStatus.MISSING,
                    byteSize = 0L,
                    downloadedAt = null,
                    lastReferencedAt = 100L,
                ),
            ),
        )
        coEvery { remoteDatabaseService.deleteSnapshot(any()) } returns 1

        repository.deleteSnapshot(
            Snapshot(
                snapshotKey = "snapshot-1",
                idUserOwner = accountId,
            ),
        )

        assertThat(offlineFeedItemDao.getBySnapshotId(accountId, "snapshot-1")).isNull()
        assertThat(offlineMediaAssetDao.getByIds(listOf("asset-1", "avatar-1"))).isEmpty()
        coVerify(exactly = 1) { remoteDatabaseService.deleteSnapshot(any()) }
    }

    private fun retainedItem(
        snapshotId: String,
        title: String,
        sortOrder: Long,
    ): OfflineFeedItemWithAssets = OfflineFeedItemWithAssets(
        accountId = accountId,
        snapshotId = snapshotId,
        ownerUserId = "owner-$snapshotId",
        title = title,
        publishedAt = 100L + sortOrder,
        sortOrder = sortOrder,
        remotePhotoUrl = "https://example.com/$snapshotId.jpg",
        mainImageAssetId = "asset-$snapshotId",
        ownerDisplayName = "Owner $snapshotId",
        ownerAvatarRemoteUrl = "https://example.com/$snapshotId-avatar.jpg",
        ownerAvatarAssetId = "avatar-$snapshotId",
        likeCount = 4,
        likedByCurrentUser = false,
        availabilityStatus = OfflineItemAvailabilityStatus.FULLY_AVAILABLE,
        syncedAt = 999L,
        mainLocalPath = "/tmp/main-$snapshotId.jpg",
        mainDownloadStatus = OfflineMediaDownloadStatus.READY,
        avatarLocalPath = "/tmp/avatar-$snapshotId.jpg",
        avatarDownloadStatus = OfflineMediaDownloadStatus.READY,
    )
}
