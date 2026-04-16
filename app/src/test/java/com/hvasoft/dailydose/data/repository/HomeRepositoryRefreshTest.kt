package com.hvasoft.dailydose.data.repository

import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.hvasoft.dailydose.data.auth.FakeAuthSessionProvider
import com.hvasoft.dailydose.data.local.FeedAssetStorage
import com.hvasoft.dailydose.data.local.FeedSyncStateDao
import com.hvasoft.dailydose.data.local.OfflineFeedItemDao
import com.hvasoft.dailydose.data.local.OfflineFeedMapper
import com.hvasoft.dailydose.data.local.OfflineMediaAssetDao
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
    private lateinit var repository: HomeRepositoryImpl
    private lateinit var authSessionProvider: FakeAuthSessionProvider

    @Before
    fun setUp() {
        val currentUser = mockk<FirebaseUser>()
        every { currentUser.uid } returns accountId
        authSessionProvider = FakeAuthSessionProvider(currentUser)

        refreshCoordinator = mockk()
        repository = HomeRepositoryImpl(
            remoteDatabaseService = mockk(relaxed = true),
            offlineFeedItemDao = mockk<OfflineFeedItemDao>(relaxed = true),
            offlineMediaAssetDao = mockk<OfflineMediaAssetDao>(relaxed = true),
            feedSyncStateDao = mockk<FeedSyncStateDao>(relaxed = true),
            offlineFeedMapper = OfflineFeedMapper(),
            refreshCoordinator = refreshCoordinator,
            authSessionProvider = authSessionProvider,
            feedAssetStorage = mockk<FeedAssetStorage>(relaxed = true),
        )
    }

    @Test
    fun `refreshSnapshots delegates retry refresh to coordinator`() = runTest {
        coEvery { refreshCoordinator.refresh(accountId) } returns Result.success(Unit)

        val result = repository.refreshSnapshots()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { refreshCoordinator.refresh(accountId) }
    }

    @Test
    fun `clearOfflineSnapshots delegates cleanup for the provided account`() = runTest {
        coJustRun { refreshCoordinator.clearAccount(accountId) }

        repository.clearOfflineSnapshots(accountId)

        coVerify(exactly = 1) { refreshCoordinator.clearAccount(accountId) }
    }

    @Test
    fun `refreshSnapshots is a no-op success when signed out`() = runTest {
        authSessionProvider.setCurrentUser(null)

        val result = repository.refreshSnapshots()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 0) { refreshCoordinator.refresh(any()) }
    }
}
