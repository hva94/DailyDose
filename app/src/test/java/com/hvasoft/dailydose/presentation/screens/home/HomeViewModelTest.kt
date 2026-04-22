package com.hvasoft.dailydose.presentation.screens.home

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.hvasoft.dailydose.MainDispatcherRule
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.auth.FakeAuthSessionProvider
import com.hvasoft.dailydose.domain.interactor.home.AddSnapshotReplyUseCase
import com.hvasoft.dailydose.domain.interactor.home.CachePostedSnapshotUseCase
import com.hvasoft.dailydose.domain.interactor.home.DeleteSnapshotUseCase
import com.hvasoft.dailydose.domain.interactor.home.GetActiveDailyPromptUseCase
import com.hvasoft.dailydose.domain.interactor.home.GetSnapshotRepliesUseCase
import com.hvasoft.dailydose.domain.interactor.home.GetSnapshotsUseCase
import com.hvasoft.dailydose.domain.interactor.home.ObservePromptCompletionUseCase
import com.hvasoft.dailydose.domain.interactor.home.SetSnapshotReactionUseCase
import com.hvasoft.dailydose.domain.model.DailyPromptAssignment
import com.hvasoft.dailydose.domain.model.DailyPromptDay
import com.hvasoft.dailydose.domain.model.HomeFeedAvailabilityMode
import com.hvasoft.dailydose.domain.model.HomeFeedLastRefreshResult
import com.hvasoft.dailydose.domain.model.HomeFeedSyncState
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.model.SnapshotReply
import com.hvasoft.dailydose.domain.model.SnapshotReplyDeliveryState
import io.mockk.coEvery
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
    private lateinit var getSnapshotRepliesUseCase: GetSnapshotRepliesUseCase
    private lateinit var addSnapshotReplyUseCase: AddSnapshotReplyUseCase
    private lateinit var setSnapshotReactionUseCase: SetSnapshotReactionUseCase
    private lateinit var deleteSnapshotUseCase: DeleteSnapshotUseCase
    private lateinit var syncStateFlow: MutableStateFlow<HomeFeedSyncState>
    private lateinit var activeDailyPromptFlow: MutableStateFlow<DailyPromptAssignment?>
    private lateinit var promptCompletionFlow: MutableStateFlow<Boolean>
    private lateinit var pagingFlow: Flow<PagingData<Snapshot>>
    private var refreshResult: Result<Unit> = Result.success(Unit)
    private lateinit var authSessionProvider: FakeAuthSessionProvider
    private lateinit var viewModel: HomeViewModel
    private var refreshCallCount = 0
    private var refreshDelayMs = 0L

    @Before
    fun setUp() {
        val currentUser = mockk<FirebaseUser>()
        every { currentUser.uid } returns accountId
        every { currentUser.displayName } returns "Henry"
        every { currentUser.email } returns "henry@example.com"
        every { currentUser.photoUrl } returns null
        authSessionProvider = FakeAuthSessionProvider(currentUser)

        syncStateFlow = MutableStateFlow(
            HomeFeedSyncState(
                availabilityMode = HomeFeedAvailabilityMode.OFFLINE_RETAINED,
                lastRefreshResult = HomeFeedLastRefreshResult.NETWORK_FAILURE,
                hasRetainedContent = true,
                retainedItemCount = 3,
            ),
        )
        activeDailyPromptFlow = MutableStateFlow(null)
        promptCompletionFlow = MutableStateFlow(false)
        pagingFlow = flowOf(PagingData.empty())
        getSnapshotRepliesUseCase = mockk()
        addSnapshotReplyUseCase = mockk()
        setSnapshotReactionUseCase = mockk(relaxed = true)
        deleteSnapshotUseCase = mockk(relaxed = true)

        viewModel = HomeViewModel(
            dispatcherIO = mainDispatcherRule.dispatcher,
            getSnapshotsUseCase = @OptIn(ExperimentalPagingApi::class) object : GetSnapshotsUseCase {
                override fun invoke(): Flow<PagingData<Snapshot>> = pagingFlow

                override fun observeSyncState(): Flow<HomeFeedSyncState> = syncStateFlow

                override suspend fun refresh(): Result<Unit> {
                    delay(refreshDelayMs)
                    refreshCallCount += 1
                    return refreshResult
                }

                override suspend fun clearOfflineSnapshots(accountId: String) = Unit
            },
            getActiveDailyPromptUseCase = object : GetActiveDailyPromptUseCase {
                override fun invoke() = activeDailyPromptFlow
            },
            observePromptCompletionUseCase = object : ObservePromptCompletionUseCase {
                override fun invoke() = promptCompletionFlow
            },
            getSnapshotRepliesUseCase = getSnapshotRepliesUseCase,
            addSnapshotReplyUseCase = addSnapshotReplyUseCase,
            cachePostedSnapshotUseCase = object : CachePostedSnapshotUseCase {
                override suspend fun invoke(snapshot: Snapshot) = Unit
            },
            setSnapshotReactionUseCase = setSnapshotReactionUseCase,
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
        assertThat(viewModel.uiState.value.canQueueInteractions).isTrue()
        collector.cancel()
    }

    @Test
    fun `uiState shows the daily prompt card when assignment exists and user has not posted`() = runTest {
        activeDailyPromptFlow.value = DailyPromptAssignment(
            dateKey = "2026-04-20",
            comboId = "daily-prompt-2",
            promptText = "What stood out today?",
            titlePatterns = listOf(
                "This stood out at %time",
                "Something stood out at %time",
            ),
            answerFormats = listOf(
                "{answer} · %time",
                "{answer} at %time",
            ),
            assignedAt = 10L,
        )
        promptCompletionFlow.value = false

        val collector = backgroundScope.launch {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.shouldShowDailyPromptCard).isTrue()
        assertThat(viewModel.uiState.value.activeDailyPrompt?.promptText)
            .isEqualTo("What stood out today?")
        collector.cancel()
    }

    @Test
    fun `cachePostedSnapshot hides the daily prompt immediately after a successful post`() = runTest {
        val postedAt = 1_713_571_200_000L
        val promptDateKey = DailyPromptDay.dateKeyFor(postedAt)
        activeDailyPromptFlow.value = DailyPromptAssignment(
            dateKey = promptDateKey,
            comboId = "daily-prompt-1",
            promptText = "What made today different?",
            titlePatterns = listOf(
                "Today felt different at %time",
                "A different moment at %time",
            ),
            answerFormats = listOf(
                "{answer} · %time",
                "{answer} at %time",
            ),
            assignedAt = 10L,
        )
        promptCompletionFlow.value = false

        val collector = backgroundScope.launch {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        viewModel.cachePostedSnapshot(
            Snapshot(
                snapshotKey = "snapshot-1",
                title = "Posted",
                dateTime = postedAt,
            ),
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.shouldShowDailyPromptCard).isFalse()
        assertThat(viewModel.uiState.value.hasPostedToday).isTrue()
        collector.cancel()
    }

    @Test
    fun `cachePostedSnapshot emits a scroll signal after the new post is cached`() = runTest {
        val initialSignal = viewModel.postPublishScrollSignal.value

        viewModel.cachePostedSnapshot(
            Snapshot(
                snapshotKey = "snapshot-1",
                title = "Posted",
                dateTime = 1_713_571_200_000L,
            ),
        )
        advanceUntilIdle()

        assertThat(viewModel.postPublishScrollSignal.value).isEqualTo(initialSignal + 1)
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
    fun `setSnapshotReaction delegates to the new use case`() = runTest {
        val snapshot = Snapshot(snapshotKey = "snapshot-1")

        viewModel.setSnapshotReaction(snapshot, "\uD83D\uDD25")
        advanceUntilIdle()

        coVerify(exactly = 1) { setSnapshotReactionUseCase.invoke(snapshot, "\uD83D\uDD25") }
    }

    @Test
    fun `setSnapshotReaction supports clearing the current reaction`() = runTest {
        val snapshot = Snapshot(snapshotKey = "snapshot-1")

        viewModel.setSnapshotReaction(snapshot, null)
        advanceUntilIdle()

        coVerify(exactly = 1) { setSnapshotReactionUseCase.invoke(snapshot, null) }
    }

    @Test
    fun `openReplies loads reply sheet content`() = runTest {
        val snapshot = Snapshot(snapshotKey = "snapshot-1", title = "Hello")
        val replies = listOf(
            SnapshotReply(
                replyId = "reply-1",
                snapshotId = "snapshot-1",
                idUserOwner = accountId,
                userName = "Henry",
                userPhotoUrl = null,
                text = "First",
                dateTime = 100L,
                deliveryState = SnapshotReplyDeliveryState.CONFIRMED,
            ),
        )
        coEvery { getSnapshotRepliesUseCase.invoke(snapshot) } returns Result.success(replies)

        viewModel.openReplies(snapshot)
        advanceUntilIdle()

        assertThat(viewModel.replySheetState.value.isVisible).isTrue()
        assertThat(viewModel.replySheetState.value.isLoading).isFalse()
        assertThat(viewModel.replySheetState.value.replies).isEqualTo(replies)
        assertThat(viewModel.replySheetState.value.errorMessageRes).isNull()
    }

    @Test
    fun `openReplies exposes the empty state when no replies are returned`() = runTest {
        val snapshot = Snapshot(snapshotKey = "snapshot-1", title = "Hello")
        coEvery { getSnapshotRepliesUseCase.invoke(snapshot) } returns Result.success(emptyList())

        viewModel.openReplies(snapshot)
        advanceUntilIdle()

        assertThat(viewModel.replySheetState.value.isVisible).isTrue()
        assertThat(viewModel.replySheetState.value.isEmpty).isTrue()
        assertThat(viewModel.replySheetState.value.errorMessageRes).isNull()
    }

    @Test
    fun `openReplies keeps the sheet open and shows a retryable load error`() = runTest {
        val snapshot = Snapshot(snapshotKey = "snapshot-1", title = "Hello")
        coEvery {
            getSnapshotRepliesUseCase.invoke(snapshot)
        } returns Result.failure(IllegalStateException("offline"))

        viewModel.openReplies(snapshot)
        advanceUntilIdle()

        assertThat(viewModel.replySheetState.value.isVisible).isTrue()
        assertThat(viewModel.replySheetState.value.isLoading).isFalse()
        assertThat(viewModel.replySheetState.value.errorMessageRes).isEqualTo(R.string.home_reply_load_error)
    }

    @Test
    fun `submitReply maps blank validation failure to composer feedback`() = runTest {
        val snapshot = Snapshot(snapshotKey = "snapshot-1")
        coEvery {
            getSnapshotRepliesUseCase.invoke(snapshot)
        } returns Result.success(emptyList())
        coEvery {
            addSnapshotReplyUseCase.invoke(snapshot, "   ")
        } returns Result.failure(IllegalArgumentException("blank"))

        viewModel.openReplies(snapshot)
        advanceUntilIdle()
        viewModel.updateReplyComposer("   ")
        viewModel.submitReply()
        advanceUntilIdle()

        assertThat(viewModel.replySheetState.value.composerMessageRes).isEqualTo(R.string.home_reply_blank_error)
        assertThat(viewModel.replySheetState.value.isSubmitting).isFalse()
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
