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
import com.hvasoft.dailydose.domain.model.PendingSnapshotActionQueueState
import com.hvasoft.dailydose.domain.model.PendingSnapshotActionType
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.model.SnapshotReply
import com.hvasoft.dailydose.domain.model.SnapshotReplyDeliveryState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
    private lateinit var offlineSnapshotReplyDao: FakeOfflineSnapshotReplyDao
    private lateinit var pendingSnapshotActionDao: FakePendingSnapshotActionDao
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
        offlineSnapshotReplyDao = FakeOfflineSnapshotReplyDao()
        pendingSnapshotActionDao = FakePendingSnapshotActionDao()
        feedSyncStateDao = FakeFeedSyncStateDao()
        remoteDatabaseService = mockk(relaxed = true)
        repository = HomeRepositoryImpl(
            remoteDatabaseService = remoteDatabaseService,
            offlineFeedItemDao = offlineFeedItemDao,
            offlineMediaAssetDao = offlineMediaAssetDao,
            offlineSnapshotReplyDao = offlineSnapshotReplyDao,
            pendingSnapshotActionDao = pendingSnapshotActionDao,
            feedSyncStateDao = feedSyncStateDao,
            offlineFeedMapper = OfflineFeedMapper(),
            refreshCoordinator = mockk(relaxed = true),
            interactionSyncCoordinator = SnapshotInteractionSyncCoordinator(
                remoteDatabaseService = remoteDatabaseService,
                pendingSnapshotActionDao = pendingSnapshotActionDao,
                offlineFeedItemDao = offlineFeedItemDao,
                offlineSnapshotReplyDao = offlineSnapshotReplyDao,
            ),
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
                reactionCount = 0,
                reactionSummary = emptyMap(),
                replyCount = 0,
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
    fun `cachePostedSnapshot normalizes legacy like fields into reaction summary defaults`() = runTest {
        coEvery { remoteDatabaseService.getUserPhotoUrlOnce(accountId) } returns ""

        repository.cachePostedSnapshot(
            Snapshot(
                title = "Legacy",
                dateTime = 300L,
                photoUrl = "https://example.com/legacy.jpg",
                snapshotKey = "legacy",
                idUserOwner = accountId,
                userName = "Henry",
                likeCount = "2",
                isLikedByCurrentUser = true,
            ),
        )

        val cachedSnapshot = offlineFeedItemDao.getBySnapshotId(accountId, "legacy")

        assertThat(cachedSnapshot?.reactionCount).isEqualTo(2)
        assertThat(cachedSnapshot?.reactionSummary).isEqualTo(mapOf("\u2764\uFE0F" to 2))
        assertThat(cachedSnapshot?.currentUserReaction).isEqualTo("\u2764\uFE0F")
    }

    @Test
    fun `setSnapshotReaction applies optimistic summary state and keeps queued work when remote sync fails`() = runTest {
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
                    likeCount = 0,
                    likedByCurrentUser = false,
                    reactionCount = 0,
                    reactionSummary = emptyMap(),
                    currentUserReaction = null,
                    replyCount = 0,
                    hasPendingReaction = false,
                    hasPendingReply = false,
                    legacyLikeCount = null,
                    availabilityStatus = OfflineItemAvailabilityStatus.MEDIA_PARTIAL,
                    syncedAt = 100L,
                ),
            ),
        )
        coEvery {
            remoteDatabaseService.setSnapshotReaction("snapshot-1", "\uD83D\uDD25")
        } throws IllegalStateException("offline")

        repository.setSnapshotReaction(
            snapshot = Snapshot(snapshotKey = "snapshot-1"),
            emoji = "\uD83D\uDD25",
        )

        val cachedSnapshot = offlineFeedItemDao.getBySnapshotId(accountId, "snapshot-1")
        val pendingAction = pendingSnapshotActionDao.getBySnapshot(accountId, "snapshot-1").single()

        assertThat(cachedSnapshot?.reactionCount).isEqualTo(1)
        assertThat(cachedSnapshot?.reactionSummary).isEqualTo(mapOf("\uD83D\uDD25" to 1))
        assertThat(cachedSnapshot?.currentUserReaction).isEqualTo("\uD83D\uDD25")
        assertThat(cachedSnapshot?.hasPendingReaction).isTrue()
        assertThat(pendingAction.actionType).isEqualTo(PendingSnapshotActionType.SET_REACTION)
        assertThat(pendingAction.queueState).isEqualTo(PendingSnapshotActionQueueState.QUEUED)
        assertThat(SnapshotInteractionSyncCoordinator.decodeReactionEmoji(pendingAction.payload))
            .isEqualTo("\uD83D\uDD25")
    }

    @Test
    fun `setSnapshotReaction removes the current reaction when the same emoji is selected again`() = runTest {
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
                    likeCount = 1,
                    likedByCurrentUser = true,
                    reactionCount = 1,
                    reactionSummary = mapOf("\u2764\uFE0F" to 1),
                    currentUserReaction = "\u2764\uFE0F",
                    replyCount = 0,
                    hasPendingReaction = false,
                    hasPendingReply = false,
                    legacyLikeCount = 1,
                    availabilityStatus = OfflineItemAvailabilityStatus.MEDIA_PARTIAL,
                    syncedAt = 100L,
                ),
            ),
        )
        coEvery { remoteDatabaseService.setSnapshotReaction("snapshot-1", null) } throws IllegalStateException("offline")

        repository.setSnapshotReaction(
            snapshot = Snapshot(
                snapshotKey = "snapshot-1",
                currentUserReaction = "\u2764\uFE0F",
                reactionCount = 1,
                reactionSummary = mapOf("\u2764\uFE0F" to 1),
            ),
            emoji = "\u2764\uFE0F",
        )

        val cachedSnapshot = offlineFeedItemDao.getBySnapshotId(accountId, "snapshot-1")
        val pendingAction = pendingSnapshotActionDao.getBySnapshot(accountId, "snapshot-1").single()

        assertThat(cachedSnapshot?.reactionCount).isEqualTo(0)
        assertThat(cachedSnapshot?.reactionSummary).isEmpty()
        assertThat(cachedSnapshot?.currentUserReaction).isNull()
        assertThat(pendingAction.actionType).isEqualTo(PendingSnapshotActionType.REMOVE_REACTION)
    }

    @Test
    fun `getSnapshotReplies merges remote and pending replies in chronological order`() = runTest {
        feedSyncStateDao.setState(
            FeedSyncStateEntity(
                accountId = accountId,
                lastSuccessfulSyncAt = 100L,
                lastRefreshAttemptAt = 100L,
                lastRefreshResult = HomeFeedLastRefreshResult.SUCCESS,
                retainedItemCount = 1,
                retentionLimit = 50,
                hasRetainedContent = true,
            ),
        )
        val remoteReplies = listOf(
            SnapshotReply(
                replyId = "reply-1",
                snapshotId = "snapshot-1",
                idUserOwner = "owner-1",
                userName = "Alex",
                userPhotoUrl = null,
                text = "First",
                dateTime = 100L,
                deliveryState = SnapshotReplyDeliveryState.CONFIRMED,
            ),
        )
        pendingSnapshotActionDao.upsert(
            com.hvasoft.dailydose.data.local.PendingSnapshotActionEntity(
                actionId = "reply-local-1",
                accountId = accountId,
                snapshotId = "snapshot-1",
                actionType = PendingSnapshotActionType.ADD_REPLY,
                payload = SnapshotInteractionSyncCoordinator.encodeReplyPayload(
                    SnapshotReply(
                        replyId = "local-1",
                        snapshotId = "snapshot-1",
                        idUserOwner = accountId,
                        userName = "Henry",
                        userPhotoUrl = null,
                        text = "Second",
                        dateTime = 200L,
                        deliveryState = SnapshotReplyDeliveryState.PENDING,
                    ),
                ),
                createdAt = 200L,
                lastAttemptAt = null,
                attemptCount = 0,
                queueState = PendingSnapshotActionQueueState.QUEUED,
                supersedesActionId = null,
            ),
        )
        coEvery { remoteDatabaseService.getSnapshotReplies("snapshot-1") } returns remoteReplies

        val result = repository.getSnapshotReplies(Snapshot(snapshotKey = "snapshot-1"))

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().map(SnapshotReply::text)).containsExactly("First", "Second").inOrder()
        assertThat(result.getOrThrow().last().deliveryState).isEqualTo(SnapshotReplyDeliveryState.PENDING)
        assertThat(offlineSnapshotReplyDao.storedReplies(accountId, "snapshot-1").map { it.text })
            .containsExactly("First")
    }

    @Test
    fun `getSnapshotReplies falls back to cached replies when offline`() = runTest {
        feedSyncStateDao.setState(
            FeedSyncStateEntity(
                accountId = accountId,
                lastSuccessfulSyncAt = 100L,
                lastRefreshAttemptAt = 200L,
                lastRefreshResult = HomeFeedLastRefreshResult.NETWORK_FAILURE,
                retainedItemCount = 1,
                retentionLimit = 50,
                hasRetainedContent = true,
            ),
        )
        offlineSnapshotReplyDao.upsertAll(
            listOf(
                com.hvasoft.dailydose.data.local.OfflineSnapshotReplyEntity(
                    accountId = accountId,
                    snapshotId = "snapshot-1",
                    replyId = "reply-1",
                    ownerUserId = "owner-1",
                    userName = "Alex",
                    userPhotoUrl = null,
                    text = "Cached reply",
                    dateTime = 100L,
                    deliveryState = SnapshotReplyDeliveryState.CONFIRMED,
                ),
            ),
        )
        coEvery { remoteDatabaseService.getSnapshotReplies("snapshot-1") } throws IllegalStateException("offline")

        val result = repository.getSnapshotReplies(Snapshot(snapshotKey = "snapshot-1"))

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().map(SnapshotReply::text)).containsExactly("Cached reply")
        coVerify(exactly = 0) { remoteDatabaseService.getSnapshotReplies(any()) }
    }

    @Test
    fun `getSnapshotReplies returns empty list offline when snapshot has no replies`() = runTest {
        feedSyncStateDao.setState(
            FeedSyncStateEntity(
                accountId = accountId,
                lastSuccessfulSyncAt = 100L,
                lastRefreshAttemptAt = 200L,
                lastRefreshResult = HomeFeedLastRefreshResult.NETWORK_FAILURE,
                retainedItemCount = 1,
                retentionLimit = 50,
                hasRetainedContent = true,
            ),
        )

        val result = repository.getSnapshotReplies(
            Snapshot(
                snapshotKey = "snapshot-1",
                replyCount = 0,
            ),
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEmpty()
        coVerify(exactly = 0) { remoteDatabaseService.getSnapshotReplies(any()) }
    }

    @Test
    fun `addSnapshotReply queues a pending reply and increments cached reply count when remote sync fails`() = runTest {
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
                    likeCount = 0,
                    likedByCurrentUser = false,
                    reactionCount = 0,
                    reactionSummary = emptyMap(),
                    currentUserReaction = null,
                    replyCount = 1,
                    hasPendingReaction = false,
                    hasPendingReply = false,
                    legacyLikeCount = null,
                    availabilityStatus = OfflineItemAvailabilityStatus.MEDIA_PARTIAL,
                    syncedAt = 100L,
                ),
            ),
        )
        coEvery { remoteDatabaseService.getUserPhotoUrlOnce(accountId) } returns "https://example.com/profile.jpg"
        coEvery {
            remoteDatabaseService.addSnapshotReply(snapshotId = "snapshot-1", reply = any())
        } throws IllegalStateException("offline")

        val result = repository.addSnapshotReply(
            snapshot = Snapshot(snapshotKey = "snapshot-1"),
            text = "  Hello there  ",
        )

        val cachedSnapshot = offlineFeedItemDao.getBySnapshotId(accountId, "snapshot-1")
        val pendingAction = pendingSnapshotActionDao.getBySnapshot(accountId, "snapshot-1").single()
        val queuedReply = SnapshotInteractionSyncCoordinator.decodeReplyPayload("snapshot-1", pendingAction.payload)

        assertThat(result.isSuccess).isTrue()
        assertThat(cachedSnapshot?.replyCount).isEqualTo(2)
        assertThat(cachedSnapshot?.hasPendingReply).isTrue()
        assertThat(pendingAction.queueState).isEqualTo(PendingSnapshotActionQueueState.QUEUED)
        assertThat(queuedReply.text).isEqualTo("Hello there")
        assertThat(queuedReply.userName).isEqualTo("Henry")
        assertThat(queuedReply.userPhotoUrl).isEqualTo("https://example.com/profile.jpg")
        assertThat(offlineSnapshotReplyDao.storedReplies(accountId, "snapshot-1").single().text)
            .isEqualTo("Hello there")
    }

    @Test
    fun `reply sync replaces cached pending reply with confirmed server reply`() = runTest {
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
                    likeCount = 0,
                    likedByCurrentUser = false,
                    reactionCount = 0,
                    reactionSummary = emptyMap(),
                    currentUserReaction = null,
                    replyCount = 1,
                    hasPendingReaction = false,
                    hasPendingReply = false,
                    legacyLikeCount = null,
                    availabilityStatus = OfflineItemAvailabilityStatus.MEDIA_PARTIAL,
                    syncedAt = 100L,
                ),
            ),
        )
        coEvery { remoteDatabaseService.getUserPhotoUrlOnce(accountId) } returns ""
        coEvery {
            remoteDatabaseService.addSnapshotReply(snapshotId = "snapshot-1", reply = any())
        } throws IllegalStateException("offline")

        repository.addSnapshotReply(
            snapshot = Snapshot(snapshotKey = "snapshot-1"),
            text = "Hello there",
        )

        val coordinator = SnapshotInteractionSyncCoordinator(
            remoteDatabaseService = remoteDatabaseService,
            pendingSnapshotActionDao = pendingSnapshotActionDao,
            offlineFeedItemDao = offlineFeedItemDao,
            offlineSnapshotReplyDao = offlineSnapshotReplyDao,
        )
        coEvery {
            remoteDatabaseService.addSnapshotReply(snapshotId = "snapshot-1", reply = any())
        } returns SnapshotReply(
            replyId = "reply-remote-1",
            snapshotId = "snapshot-1",
            idUserOwner = accountId,
            userName = "Henry",
            userPhotoUrl = null,
            text = "Hello there",
            dateTime = 200L,
            deliveryState = SnapshotReplyDeliveryState.CONFIRMED,
        )

        coordinator.flushPendingActions(accountId = accountId, rollbackOnFailure = true)

        val cachedReplies = offlineSnapshotReplyDao.storedReplies(accountId, "snapshot-1")
        assertThat(cachedReplies).hasSize(1)
        assertThat(cachedReplies.single().replyId).isEqualTo("reply-remote-1")
        assertThat(cachedReplies.single().deliveryState).isEqualTo(SnapshotReplyDeliveryState.CONFIRMED)
        assertThat(pendingSnapshotActionDao.getBySnapshot(accountId, "snapshot-1")).isEmpty()
    }

    @Test
    fun `reply rollback removes cached pending reply after reconnect failure`() = runTest {
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
                    likeCount = 0,
                    likedByCurrentUser = false,
                    reactionCount = 0,
                    reactionSummary = emptyMap(),
                    currentUserReaction = null,
                    replyCount = 1,
                    hasPendingReaction = false,
                    hasPendingReply = false,
                    legacyLikeCount = null,
                    availabilityStatus = OfflineItemAvailabilityStatus.MEDIA_PARTIAL,
                    syncedAt = 100L,
                ),
            ),
        )
        coEvery { remoteDatabaseService.getUserPhotoUrlOnce(accountId) } returns ""
        coEvery {
            remoteDatabaseService.addSnapshotReply(snapshotId = "snapshot-1", reply = any())
        } throws IllegalStateException("offline")

        repository.addSnapshotReply(
            snapshot = Snapshot(snapshotKey = "snapshot-1"),
            text = "Hello there",
        )

        val coordinator = SnapshotInteractionSyncCoordinator(
            remoteDatabaseService = remoteDatabaseService,
            pendingSnapshotActionDao = pendingSnapshotActionDao,
            offlineFeedItemDao = offlineFeedItemDao,
            offlineSnapshotReplyDao = offlineSnapshotReplyDao,
        )

        coordinator.flushPendingActions(accountId = accountId, rollbackOnFailure = true)

        assertThat(offlineSnapshotReplyDao.storedReplies(accountId, "snapshot-1")).isEmpty()
        assertThat(pendingSnapshotActionDao.getBySnapshot(accountId, "snapshot-1").single().queueState)
            .isEqualTo(PendingSnapshotActionQueueState.FAILED)
    }

    @Test
    fun `addSnapshotReply rejects blank replies before queueing`() = runTest {
        val result = repository.addSnapshotReply(
            snapshot = Snapshot(snapshotKey = "snapshot-1"),
            text = "   ",
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(pendingSnapshotActionDao.getByAccount(accountId)).isEmpty()
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
                    likeCount = 0,
                    likedByCurrentUser = false,
                    reactionCount = 0,
                    reactionSummary = emptyMap(),
                    currentUserReaction = null,
                    replyCount = 0,
                    hasPendingReaction = false,
                    hasPendingReply = false,
                    legacyLikeCount = null,
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
        reactionCount = 4,
        reactionSummary = mapOf("\u2764\uFE0F" to 4),
        currentUserReaction = null,
        replyCount = 2,
        hasPendingReaction = false,
        hasPendingReply = false,
        legacyLikeCount = 4,
        availabilityStatus = OfflineItemAvailabilityStatus.FULLY_AVAILABLE,
        syncedAt = 999L,
        mainLocalPath = "/tmp/main-$snapshotId.jpg",
        mainDownloadStatus = OfflineMediaDownloadStatus.READY,
        avatarLocalPath = "/tmp/avatar-$snapshotId.jpg",
        avatarDownloadStatus = OfflineMediaDownloadStatus.READY,
    )
}
