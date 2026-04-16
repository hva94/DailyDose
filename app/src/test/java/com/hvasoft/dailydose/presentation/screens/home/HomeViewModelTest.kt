package com.hvasoft.dailydose.presentation.screens.home

import androidx.paging.ExperimentalPagingApi
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.hvasoft.dailydose.MainDispatcherRule
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.auth.FakeAuthSessionProvider
import com.hvasoft.dailydose.domain.interactor.home.CachePostedSnapshotUseCase
import com.hvasoft.dailydose.domain.interactor.home.DeleteSnapshotUseCase
import com.hvasoft.dailydose.domain.interactor.home.GetSnapshotsUseCase
import com.hvasoft.dailydose.domain.interactor.home.ToggleUserLikeUseCase
import com.hvasoft.dailydose.domain.model.HomeFeedAvailabilityMode
import com.hvasoft.dailydose.domain.model.HomeFeedLastRefreshResult
import com.hvasoft.dailydose.domain.model.HomeFeedSyncState
import com.hvasoft.dailydose.domain.model.Snapshot
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountId = "user-123"
    private lateinit var toggleUserLikeUseCase: ToggleUserLikeUseCase
    private lateinit var deleteSnapshotUseCase: DeleteSnapshotUseCase
    private lateinit var syncStateFlow: MutableStateFlow<HomeFeedSyncState>
    private lateinit var pagingFlow: Flow<androidx.paging.PagingData<Snapshot>>
    private var refreshResult: Result<Unit> = Result.success(Unit)
    private lateinit var authSessionProvider: FakeAuthSessionProvider
    private lateinit var viewModel: HomeViewModel
    private var refreshCallCount = 0
    private var refreshDelayMs = 0L

    @Before
    fun setUp() {
        val currentUser = mockk<FirebaseUser>()
        every { currentUser.uid } returns accountId
        authSessionProvider = FakeAuthSessionProvider(currentUser)

        syncStateFlow = MutableStateFlow(
            HomeFeedSyncState(
                availabilityMode = HomeFeedAvailabilityMode.OFFLINE_RETAINED,
                lastRefreshResult = HomeFeedLastRefreshResult.NETWORK_FAILURE,
                hasRetainedContent = true,
                retainedItemCount = 3,
            ),
        )
        pagingFlow = flowOf(androidx.paging.PagingData.empty())
        toggleUserLikeUseCase = mockk(relaxed = true)
        deleteSnapshotUseCase = mockk(relaxed = true)

        viewModel = HomeViewModel(
            dispatcherIO = mainDispatcherRule.dispatcher,
            getSnapshotsUseCase = @OptIn(ExperimentalPagingApi::class) object : GetSnapshotsUseCase {
                override fun invoke(): Flow<androidx.paging.PagingData<Snapshot>> = pagingFlow

                override fun observeSyncState(): kotlinx.coroutines.flow.Flow<HomeFeedSyncState> = syncStateFlow

                override suspend fun refresh(): Result<Unit> {
                    delay(refreshDelayMs)
                    refreshCallCount += 1
                    return refreshResult
                }

                override suspend fun clearOfflineSnapshots(accountId: String) = Unit
            },
            cachePostedSnapshotUseCase = object : CachePostedSnapshotUseCase {
                override suspend fun invoke(snapshot: Snapshot) = Unit
            },
            toggleUserLikeUseCase = toggleUserLikeUseCase,
            deleteSnapshotUseCase = deleteSnapshotUseCase,
            authSessionProvider = authSessionProvider,
        )
    }

    @Test
    fun `uiState reflects offline retained mode and read only action policy`() = runTest {
        val collector = backgroundScope.launch {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.availabilityMode).isEqualTo(HomeFeedAvailabilityMode.OFFLINE_RETAINED)
        assertThat(viewModel.uiState.value.actionPolicy).isEqualTo(HomeFeedActionPolicy.READ_ONLY_OFFLINE)
        collector.cancel()
    }

    @Test
    fun `retrySync emits connectivity error when refresh fails`() = runTest {
        refreshResult = Result.failure(IllegalStateException("offline"))
        val event = async { viewModel.events.first() }
        advanceUntilIdle()

        viewModel.retrySync()
        advanceUntilIdle()

        assertThat(event.await()).isEqualTo(R.string.error_connectivity)
    }

    @Test
    fun `setLikeSnapshot blocks remote mutation while offline`() = runTest {
        val event = async { viewModel.events.first() }
        advanceUntilIdle()

        viewModel.setLikeSnapshot(Snapshot(snapshotKey = "snapshot-1"), isChecked = true)
        advanceUntilIdle()

        assertThat(event.await()).isEqualTo(R.string.home_like_offline_unavailable)
        coVerify(exactly = 0) { toggleUserLikeUseCase.invoke(any(), any()) }
    }

    @Test
    fun `fetchSnapshots and retrySync no-op when signed out`() = runTest {
        authSessionProvider.setCurrentUser(null)
        advanceUntilIdle()

        viewModel.fetchSnapshots()
        viewModel.retrySync()
        advanceUntilIdle()

        assertThat(refreshCallCount).isEqualTo(0)
    }

    @Test
    fun `uiState stays full access while an online refresh is in flight`() = runTest {
        syncStateFlow.value = HomeFeedSyncState(
            availabilityMode = HomeFeedAvailabilityMode.ONLINE_FRESH,
            lastSuccessfulSyncAt = 1_000L,
            lastRefreshResult = HomeFeedLastRefreshResult.SUCCESS,
            hasRetainedContent = true,
            retainedItemCount = 3,
        )
        val collector = backgroundScope.launch {
            viewModel.uiState.collect { }
        }

        viewModel.retrySync()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.actionPolicy).isEqualTo(HomeFeedActionPolicy.FULL_ACCESS)
        collector.cancel()
    }

    @Test
    fun `shouldAutoRefreshOnResume returns true only when the last successful sync is stale`() = runTest {
        syncStateFlow.value = HomeFeedSyncState(
            availabilityMode = HomeFeedAvailabilityMode.ONLINE_FRESH,
            lastSuccessfulSyncAt = 1_000L,
            lastRefreshResult = HomeFeedLastRefreshResult.SUCCESS,
            hasRetainedContent = true,
        )
        val collector = backgroundScope.launch {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        assertThat(viewModel.shouldAutoRefreshOnResume(nowMillis = 1_000L + 600_001L)).isTrue()
        assertThat(viewModel.shouldAutoRefreshOnResume(nowMillis = 1_000L + 60_000L)).isFalse()
        collector.cancel()
    }

    @Test
    fun `auto refresh keeps the pull indicator hidden before the visibility threshold`() = runTest {
        refreshDelayMs = 900L
        syncStateFlow.value = HomeFeedSyncState(
            availabilityMode = HomeFeedAvailabilityMode.ONLINE_FRESH,
            lastSuccessfulSyncAt = 1_000L,
            lastRefreshResult = HomeFeedLastRefreshResult.SUCCESS,
            hasRetainedContent = true,
            retainedItemCount = 3,
        )
        val collector = backgroundScope.launch {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        viewModel.fetchSnapshots(reloadSource = false)
        advanceTimeBy(500L)

        assertThat(viewModel.uiState.value.isBackgroundRefreshing).isTrue()
        assertThat(viewModel.uiState.value.showsRefreshIndicator).isFalse()

        advanceUntilIdle()
        collector.cancel()
    }

    @Test
    fun `auto refresh shows the pull indicator when the refresh exceeds the threshold`() = runTest {
        refreshDelayMs = 1_500L
        syncStateFlow.value = HomeFeedSyncState(
            availabilityMode = HomeFeedAvailabilityMode.ONLINE_FRESH,
            lastSuccessfulSyncAt = 1_000L,
            lastRefreshResult = HomeFeedLastRefreshResult.SUCCESS,
            hasRetainedContent = true,
            retainedItemCount = 3,
        )
        val collector = backgroundScope.launch {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        viewModel.fetchSnapshots(reloadSource = false)
        advanceTimeBy(1_001L)

        assertThat(viewModel.uiState.value.isBackgroundRefreshing).isTrue()
        assertThat(viewModel.uiState.value.showsRefreshIndicator).isTrue()

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.showsRefreshIndicator).isFalse()
        collector.cancel()
    }

    @Test
    fun `manual refresh shows the pull indicator immediately`() = runTest {
        refreshDelayMs = 1_500L
        syncStateFlow.value = HomeFeedSyncState(
            availabilityMode = HomeFeedAvailabilityMode.ONLINE_FRESH,
            lastSuccessfulSyncAt = 1_000L,
            lastRefreshResult = HomeFeedLastRefreshResult.SUCCESS,
            hasRetainedContent = true,
            retainedItemCount = 3,
        )
        val collector = backgroundScope.launch {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        viewModel.retrySync()
        advanceTimeBy(1L)

        assertThat(viewModel.uiState.value.isBackgroundRefreshing).isTrue()
        assertThat(viewModel.uiState.value.showsRefreshIndicator).isTrue()

        advanceUntilIdle()
        collector.cancel()
    }
}
