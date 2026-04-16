package com.hvasoft.dailydose.data.repository

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.hvasoft.dailydose.MainDispatcherRule
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.data.local.FeedAssetStorage
import com.hvasoft.dailydose.domain.model.HomeFeedLastRefreshResult
import com.hvasoft.dailydose.domain.model.Snapshot
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class HomeFeedRefreshCoordinatorTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountId = "user-123"
    private lateinit var tempDir: File
    private lateinit var coordinator: HomeFeedRefreshCoordinator
    private lateinit var offlineFeedItemDao: FakeOfflineFeedItemDao
    private lateinit var offlineMediaAssetDao: FakeOfflineMediaAssetDao
    private lateinit var feedSyncStateDao: FakeFeedSyncStateDao

    @Before
    fun setUp() {
        tempDir = createTempDir(prefix = "offline-feed-test")
        val context = mockk<Context>()
        io.mockk.every { context.filesDir } returns tempDir

        offlineFeedItemDao = FakeOfflineFeedItemDao()
        offlineMediaAssetDao = FakeOfflineMediaAssetDao()
        feedSyncStateDao = FakeFeedSyncStateDao()

        val mediaSource = File(tempDir, "seed-image.jpg").apply {
            writeText("offline image seed")
        }
        val mediaUrl = mediaSource.toURI().toURL().toString()
        val snapshots = (0 until 60).map { index ->
            Snapshot(
                title = "Snapshot $index",
                dateTime = 1_000L - index,
                photoUrl = mediaUrl,
                idUserOwner = "owner-$index",
                snapshotKey = "snapshot-$index",
                likeCount = index.toString(),
            )
        }

        coordinator = HomeFeedRefreshCoordinator(
            remoteDatabaseService = FakeRemoteDatabaseService(
                snapshots = snapshots,
                namesByUserId = snapshots.associate { it.idUserOwner.orEmpty() to "User ${it.snapshotKey}" },
                avatarsByUserId = snapshots.associate { it.idUserOwner.orEmpty() to mediaUrl },
            ),
            offlineFeedItemDao = offlineFeedItemDao,
            offlineMediaAssetDao = offlineMediaAssetDao,
            feedSyncStateDao = feedSyncStateDao,
            feedAssetStorage = FeedAssetStorage(context, mainDispatcherRule.dispatcher),
            dispatcherIO = mainDispatcherRule.dispatcher,
        )
    }

    @Test
    fun `refresh retains latest fifty feed items and marks sync successful`() = runTest {
        val result = coordinator.refresh(accountId)

        assertThat(result.isSuccess).isTrue()
        assertThat(offlineFeedItemDao.storedItems(accountId)).hasSize(Constants.OFFLINE_FEED_RETENTION_LIMIT)
        assertThat(feedSyncStateDao.currentState(accountId)?.lastRefreshResult)
            .isEqualTo(HomeFeedLastRefreshResult.SUCCESS)
        assertThat(offlineMediaAssetDao.storedAssets(accountId)).isNotEmpty()
    }
}
