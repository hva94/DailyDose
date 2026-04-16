package com.hvasoft.dailydose.presentation.screens.home

import androidx.paging.ExperimentalPagingApi
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.hvasoft.dailydose.MainDispatcherRule
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.auth.FakeAuthSessionProvider
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
                    refreshCallCount += 1
                    return refreshResult
                }

                override suspend fun clearOfflineSnapshots(accountId: String) = Unit
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
}
