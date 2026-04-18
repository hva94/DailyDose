package com.hvasoft.dailydose.data.repository

import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.hvasoft.dailydose.data.auth.FakeAuthSessionProvider
import com.hvasoft.dailydose.data.local.FeedAssetStorage
import com.hvasoft.dailydose.data.local.FeedSyncStateDao
import com.hvasoft.dailydose.data.local.OfflineFeedItemDao
import com.hvasoft.dailydose.data.local.OfflineFeedMapper
import com.hvasoft.dailydose.data.local.OfflineMediaAssetDao
import com.hvasoft.dailydose.data.local.PendingSnapshotActionDao
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeRepositoryRefreshTest {

    private val accountId = "user-123"
    private lateinit var refreshCoordinator: HomeFeedRefreshCoordinator
    private lateinit var interactionSyncCoordinator: SnapshotInteractionSyncCoordinator
    private lateinit var pendingSnapshotActionDao: PendingSnapshotActionDao
    private lateinit var repository: HomeRepositoryImpl
    private lateinit var authSessionProvider: FakeAuthSessionProvider

    @Before
    fun setUp() {
        val currentUser = mockk<FirebaseUser>()
        every { currentUser.uid } returns accountId
        authSessionProvider = FakeAuthSessionProvider(currentUser)

        refreshCoordinator = mockk()
        interactionSyncCoordinator = mockk()
        pendingSnapshotActionDao = mockk(relaxed = true)
        repository = HomeRepositoryImpl(
            remoteDatabaseService = mockk(relaxed = true),
            offlineFeedItemDao = mockk<OfflineFeedItemDao>(relaxed = true),
            offlineMediaAssetDao = mockk<OfflineMediaAssetDao>(relaxed = true),
            pendingSnapshotActionDao = pendingSnapshotActionDao,
            feedSyncStateDao = mockk<FeedSyncStateDao>(relaxed = true),
            offlineFeedMapper = OfflineFeedMapper(),
            refreshCoordinator = refreshCoordinator,
            interactionSyncCoordinator = interactionSyncCoordinator,
            authSessionProvider = authSessionProvider,
            feedAssetStorage = mockk<FeedAssetStorage>(relaxed = true),
        )
    }

    @Test
    fun `refreshSnapshots flushes pending interactions after a successful refresh`() = runTest {
        coEvery { refreshCoordinator.refresh(accountId) } returns Result.success(Unit)
        coEvery {
            interactionSyncCoordinator.flushPendingActions(
                accountId = accountId,
                rollbackOnFailure = true,
            )
        } returns SnapshotInteractionSyncCoordinator.SyncResult()

        val result = repository.refreshSnapshots()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { refreshCoordinator.refresh(accountId) }
        coVerify(exactly = 1) {
            interactionSyncCoordinator.flushPendingActions(
                accountId = accountId,
                rollbackOnFailure = true,
            )
        }
    }

    @Test
    fun `refreshSnapshots performs a second refresh when queued interactions changed feed state`() = runTest {
        coEvery { refreshCoordinator.refresh(accountId) } returns Result.success(Unit)
        coEvery {
            interactionSyncCoordinator.flushPendingActions(
                accountId = accountId,
                rollbackOnFailure = true,
            )
        } returns SnapshotInteractionSyncCoordinator.SyncResult(applied = true)

        val result = repository.refreshSnapshots()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 2) { refreshCoordinator.refresh(accountId) }
    }

    @Test
    fun `clearOfflineSnapshots delegates cleanup for the provided account and pending actions`() = runTest {
        coJustRun { refreshCoordinator.clearAccount(accountId) }
        coJustRun { pendingSnapshotActionDao.deleteByAccount(accountId) }

        repository.clearOfflineSnapshots(accountId)

        coVerify(exactly = 1) { refreshCoordinator.clearAccount(accountId) }
        coVerify(exactly = 1) { pendingSnapshotActionDao.deleteByAccount(accountId) }
    }

    @Test
    fun `refreshSnapshots is a no-op success when signed out`() = runTest {
        authSessionProvider.setCurrentUser(null)

        val result = repository.refreshSnapshots()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 0) { refreshCoordinator.refresh(any()) }
        coVerify(exactly = 0) { interactionSyncCoordinator.flushPendingActions(any(), any()) }
    }
}
