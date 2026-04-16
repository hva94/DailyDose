package com.hvasoft.dailydose.data.repository

import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.hvasoft.dailydose.MainDispatcherRule
import com.hvasoft.dailydose.data.auth.FakeAuthSessionProvider
import com.hvasoft.dailydose.data.local.FeedSyncStateEntity
import com.hvasoft.dailydose.data.local.OfflineFeedItemWithAssets
import com.hvasoft.dailydose.data.local.OfflineFeedMapper
import com.hvasoft.dailydose.data.local.OfflineItemAvailabilityStatus
import com.hvasoft.dailydose.data.local.OfflineMediaDownloadStatus
import com.hvasoft.dailydose.domain.model.HomeFeedAvailabilityMode
import com.hvasoft.dailydose.domain.model.HomeFeedLastRefreshResult
import io.mockk.mockk
import io.mockk.every
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
    private lateinit var feedSyncStateDao: FakeFeedSyncStateDao
    private lateinit var repository: HomeRepositoryImpl
    private lateinit var authSessionProvider: FakeAuthSessionProvider

    @Before
    fun setUp() {
        val currentUser = mockk<FirebaseUser>()
        every { currentUser.uid } returns accountId
        authSessionProvider = FakeAuthSessionProvider(currentUser)

        offlineFeedItemDao = FakeOfflineFeedItemDao()
        feedSyncStateDao = FakeFeedSyncStateDao()
        repository = HomeRepositoryImpl(
            remoteDatabaseService = mockk(relaxed = true),
            offlineFeedItemDao = offlineFeedItemDao,
            feedSyncStateDao = feedSyncStateDao,
            offlineFeedMapper = OfflineFeedMapper(),
            refreshCoordinator = mockk(relaxed = true),
            authSessionProvider = authSessionProvider,
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
