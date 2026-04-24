package com.hvasoft.dailydose.data.network.data_source

import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import com.hvasoft.dailydose.data.auth.FakeAuthSessionProvider
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.data.config.DailyPromptCatalogProvider
import com.hvasoft.dailydose.data.network.model.DailyPromptAssignmentDTO
import com.hvasoft.dailydose.data.network.model.SnapshotRevealRecordDTO
import com.hvasoft.dailydose.data.network.model.SnapshotReactionDTO
import com.hvasoft.dailydose.data.network.model.SnapshotReplyDTO
import com.hvasoft.dailydose.domain.model.CreateSnapshotRequest
import com.hvasoft.dailydose.domain.model.CreateSnapshotResult
import com.hvasoft.dailydose.domain.model.DailyPromptAssignment
import com.hvasoft.dailydose.domain.model.DailyPromptCombo
import com.hvasoft.dailydose.domain.model.DailyPromptComboSelector
import com.hvasoft.dailydose.domain.model.DailyPromptDay
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.model.SnapshotReply
import com.hvasoft.dailydose.domain.model.SnapshotReplyDeliveryState
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteDatabaseServiceImplTest {

    private lateinit var snapshotsDatabase: DatabaseReference
    private lateinit var usersDatabase: DatabaseReference
    private lateinit var snapshotsRootStorage: StorageReference
    private lateinit var authSessionProvider: FakeAuthSessionProvider
    private lateinit var dailyPromptCatalogProvider: DailyPromptCatalogProvider
    private lateinit var service: RemoteDatabaseServiceImpl
    private lateinit var dailyPromptCombos: List<DailyPromptCombo>

    @Before
    fun setUp() {
        snapshotsDatabase = mockk(relaxed = true)
        usersDatabase = mockk(relaxed = true)
        snapshotsRootStorage = mockk(relaxed = true)
        authSessionProvider = FakeAuthSessionProvider()
        dailyPromptCombos = listOf(
            DailyPromptCombo(
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
            ),
            DailyPromptCombo(
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
            ),
        )
        dailyPromptCatalogProvider = object : DailyPromptCatalogProvider {
            override fun getDailyPromptCombos(): List<DailyPromptCombo> = dailyPromptCombos
        }
        service = RemoteDatabaseServiceImpl(
            snapshotsDatabase = snapshotsDatabase,
            usersDatabase = usersDatabase,
            snapshotsRootStorage = snapshotsRootStorage,
            authSessionProvider = authSessionProvider,
            dailyPromptCatalogProvider = dailyPromptCatalogProvider,
        )
    }

    @Test
    fun `construction does not require a signed in user`() {
        assertThat(service).isNotNull()
    }

    @Test
    fun `publishSnapshot returns save failed when no signed in user exists`() = runTest {
        val result = service.publishSnapshot(
            request = CreateSnapshotRequest(
                localImageContentUri = "content://images/test",
                title = "Test",
            ),
            onProgress = {},
        )

        assertThat(result).isEqualTo(CreateSnapshotResult.SaveFailed)
    }

    @Test
    fun `observeActiveDailyPrompt persists title patterns and answer formats from the provider`() = runTest {
        val dateKey = DailyPromptDay.currentDateKey()
        val previousDateKey = DailyPromptDay.previousDateKey(dateKey)
        val rootReference = mockk<DatabaseReference>()
        val assignmentsReference = mockk<DatabaseReference>()
        val promptReference = mockk<DatabaseReference>()
        val previousPromptReference = mockk<DatabaseReference>()
        val currentPromptSnapshot = mockk<DataSnapshot>()
        val previousPromptSnapshot = mockk<DataSnapshot>()
        val storedAssignment = slot<DailyPromptAssignmentDTO>()

        every { snapshotsDatabase.root } returns rootReference
        every { rootReference.child(Constants.DAILY_PROMPT_ASSIGNMENTS_PATH) } returns assignmentsReference
        every { assignmentsReference.child(dateKey) } returns promptReference
        every { assignmentsReference.child(previousDateKey) } returns previousPromptReference
        every { promptReference.get() } returns Tasks.forResult(currentPromptSnapshot)
        every { previousPromptReference.get() } returns Tasks.forResult(previousPromptSnapshot)
        every { currentPromptSnapshot.getValue(DailyPromptAssignmentDTO::class.java) } returns null
        every { previousPromptSnapshot.getValue(DailyPromptAssignmentDTO::class.java) } returns null
        every { promptReference.setValue(capture(storedAssignment)) } returns Tasks.forResult(null)
        every { promptReference.addValueEventListener(any()) } answers { firstArg<ValueEventListener>() }
        every { promptReference.removeEventListener(any<ValueEventListener>()) } returns Unit

        val observedPrompt = service.observeActiveDailyPrompt().first()
        val expectedCombo = DailyPromptComboSelector.resolveForDay(
            combos = dailyPromptCombos,
            dateKey = dateKey,
            previousComboId = null,
        )!!

        assertThat(observedPrompt?.comboId).isEqualTo(expectedCombo.comboId)
        assertThat(observedPrompt?.titlePatterns).containsExactlyElementsIn(expectedCombo.titlePatterns).inOrder()
        assertThat(observedPrompt?.answerFormats).containsExactlyElementsIn(expectedCombo.answerFormats).inOrder()
        assertThat(storedAssignment.captured.titlePatterns)
            .containsExactlyElementsIn(expectedCombo.titlePatterns)
            .inOrder()
        assertThat(storedAssignment.captured.answerFormats)
            .containsExactlyElementsIn(expectedCombo.answerFormats)
            .inOrder()
    }

    @Test
    fun `observeActiveDailyPrompt falls back to the local prompt when remote access is denied`() = runTest {
        val dateKey = DailyPromptDay.currentDateKey()
        val rootReference = mockk<DatabaseReference>()
        val assignmentsReference = mockk<DatabaseReference>()
        val promptReference = mockk<DatabaseReference>()

        every { snapshotsDatabase.root } returns rootReference
        every { rootReference.child(Constants.DAILY_PROMPT_ASSIGNMENTS_PATH) } returns assignmentsReference
        every { assignmentsReference.child(dateKey) } returns promptReference
        every { promptReference.get() } returns Tasks.forException(DatabaseException("Permission denied"))
        every { promptReference.addValueEventListener(any()) } answers { firstArg<ValueEventListener>() }
        every { promptReference.removeEventListener(any<ValueEventListener>()) } returns Unit

        val observedPrompt = service.observeActiveDailyPrompt().first()
        val expectedCombo = DailyPromptComboSelector.resolveForDay(
            combos = dailyPromptCombos,
            dateKey = dateKey,
            previousComboId = null,
        )!!

        assertThat(observedPrompt?.comboId).isEqualTo(expectedCombo.comboId)
        assertThat(observedPrompt?.promptText).isEqualTo(expectedCombo.promptText)
    }

    @Test
    fun `observeActiveDailyPrompt completes without failure when listener is cancelled`() = runTest {
        val dateKey = DailyPromptDay.currentDateKey()
        val previousDateKey = DailyPromptDay.previousDateKey(dateKey)
        val rootReference = mockk<DatabaseReference>()
        val assignmentsReference = mockk<DatabaseReference>()
        val promptReference = mockk<DatabaseReference>()
        val previousPromptReference = mockk<DatabaseReference>()
        val currentPromptSnapshot = mockk<DataSnapshot>()
        val previousPromptSnapshot = mockk<DataSnapshot>()
        val listenerSlot = slot<ValueEventListener>()
        val permissionDeniedError = mockk<DatabaseError>()

        every { snapshotsDatabase.root } returns rootReference
        every { rootReference.child(Constants.DAILY_PROMPT_ASSIGNMENTS_PATH) } returns assignmentsReference
        every { assignmentsReference.child(dateKey) } returns promptReference
        every { assignmentsReference.child(previousDateKey) } returns previousPromptReference
        every { promptReference.get() } returns Tasks.forResult(currentPromptSnapshot)
        every { previousPromptReference.get() } returns Tasks.forResult(previousPromptSnapshot)
        every { currentPromptSnapshot.getValue(DailyPromptAssignmentDTO::class.java) } returns null
        every { previousPromptSnapshot.getValue(DailyPromptAssignmentDTO::class.java) } returns null
        every { promptReference.setValue(any()) } returns Tasks.forResult(null)
        every { promptReference.addValueEventListener(capture(listenerSlot)) } answers { listenerSlot.captured }
        every { promptReference.removeEventListener(any<ValueEventListener>()) } returns Unit
        every { permissionDeniedError.message } returns "Permission denied"

        val observedPrompts = mutableListOf<DailyPromptAssignment?>()
        val collectJob = launch {
            service.observeActiveDailyPrompt().toList(observedPrompts)
        }

        advanceUntilIdle()
        listenerSlot.captured.onCancelled(permissionDeniedError)
        collectJob.join()

        assertThat(collectJob.isCancelled).isFalse()
        assertThat(observedPrompts).isNotEmpty()
    }

    @Test
    fun `setSnapshotReaction throws a controlled error when no signed in user exists`() = runTest {
        val failure = runCatching {
            service.setSnapshotReaction(
                snapshotId = "snapshot-1",
                emoji = "\uD83D\uDD25",
            )
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `getRevealedSnapshots returns only the requested entries for the current viewer`() = runTest {
        val currentUser = mockk<FirebaseUser>()
        every { currentUser.uid } returns "user-123"
        authSessionProvider.setCurrentUser(currentUser)

        val userReference = mockk<DatabaseReference>()
        val revealsReference = mockk<DatabaseReference>()
        val revealsSnapshot = mockk<DataSnapshot>()
        val firstReveal = mockk<DataSnapshot>()
        val secondReveal = mockk<DataSnapshot>()

        every { usersDatabase.child("user-123") } returns userReference
        every { userReference.child(Constants.USER_REVEALED_SNAPSHOTS_PATH) } returns revealsReference
        every { revealsReference.get() } returns Tasks.forResult(revealsSnapshot)
        every { revealsSnapshot.exists() } returns true
        every { revealsSnapshot.children } returns listOf(firstReveal, secondReveal)
        every { firstReveal.key } returns "snapshot-1"
        every { secondReveal.key } returns "snapshot-2"
        every { firstReveal.getValue(SnapshotRevealRecordDTO::class.java) } returns SnapshotRevealRecordDTO(revealedAt = 111L)
        every { secondReveal.getValue(SnapshotRevealRecordDTO::class.java) } returns SnapshotRevealRecordDTO(revealedAt = 222L)

        val result = service.getRevealedSnapshots(setOf("snapshot-1"))

        assertThat(result).containsExactly("snapshot-1", 111L)
    }

    @Test
    fun `markSnapshotRevealed preserves an existing reveal timestamp`() = runTest {
        val currentUser = mockk<FirebaseUser>()
        every { currentUser.uid } returns "user-123"
        authSessionProvider.setCurrentUser(currentUser)

        val userReference = mockk<DatabaseReference>()
        val revealsReference = mockk<DatabaseReference>()
        val snapshotRevealReference = mockk<DatabaseReference>()
        val existingSnapshot = mockk<DataSnapshot>()

        every { usersDatabase.child("user-123") } returns userReference
        every { userReference.child(Constants.USER_REVEALED_SNAPSHOTS_PATH) } returns revealsReference
        every { revealsReference.child("snapshot-1") } returns snapshotRevealReference
        every { snapshotRevealReference.get() } returns Tasks.forResult(existingSnapshot)
        every { existingSnapshot.getValue(SnapshotRevealRecordDTO::class.java) } returns SnapshotRevealRecordDTO(revealedAt = 111L)
        every { snapshotRevealReference.setValue(any()) } returns Tasks.forResult(null)

        service.markSnapshotRevealed(snapshotId = "snapshot-1", revealedAt = 999L)

        verify(exactly = 1) {
            snapshotRevealReference.setValue(
                withArg<SnapshotRevealRecordDTO> { revealRecord ->
                    assertThat(revealRecord.revealedAt).isEqualTo(111L)
                },
            )
        }
    }

    @Test
    fun `setSnapshotReaction writes the user reaction and summary fields`() = runTest {
        val currentUser = mockk<FirebaseUser>()
        every { currentUser.uid } returns "user-123"
        authSessionProvider.setCurrentUser(currentUser)

        val snapshotReference = mockk<DatabaseReference>()
        val reactionsReference = mockk<DatabaseReference>()
        val reactionUserReference = mockk<DatabaseReference>()
        val summaryReference = mockk<DatabaseReference>()
        val countReference = mockk<DatabaseReference>()
        val likeListReference = mockk<DatabaseReference>()
        val dateTimeSnapshot = mockk<DataSnapshot>()
        val likeListSnapshot = mockk<DataSnapshot>()
        val reactionsSnapshot = mockk<DataSnapshot>()
        val snapshotState = mockk<DataSnapshot>()

        every { snapshotsDatabase.child("snapshot-1") } returns snapshotReference
        every { snapshotReference.get() } returns Tasks.forResult(snapshotState)
        every { snapshotReference.child(Constants.REACTIONS_PATH) } returns reactionsReference
        every { reactionsReference.child("user-123") } returns reactionUserReference
        every { snapshotReference.child(Constants.REACTION_SUMMARY_PROPERTY) } returns summaryReference
        every { snapshotReference.child(Constants.REACTION_COUNT_PROPERTY) } returns countReference
        every { snapshotReference.child(Constants.LIKE_LIST_PROPERTY) } returns likeListReference
        every { reactionUserReference.setValue(any()) } returns Tasks.forResult(null)
        every { summaryReference.setValue(any()) } returns Tasks.forResult(null)
        every { countReference.setValue(any()) } returns Tasks.forResult(null)
        every { likeListReference.child("user-123") } returns mockk(relaxed = true)
        every { snapshotState.child("dateTime") } returns dateTimeSnapshot
        every { snapshotState.child(Constants.LIKE_LIST_PROPERTY) } returns likeListSnapshot
        every { snapshotState.child(Constants.REACTIONS_PATH) } returns reactionsSnapshot
        every { dateTimeSnapshot.getValue(Long::class.java) } returns 100L
        every { likeListSnapshot.children } returns emptyList()
        every { likeListSnapshot.hasChild("user-123") } returns false
        every { reactionsSnapshot.children } returns emptyList()

        val result = service.setSnapshotReaction("snapshot-1", "\uD83D\uDD25")

        assertThat(result).isEqualTo(1)
        verify(exactly = 1) {
            reactionUserReference.setValue(
                withArg<SnapshotReactionDTO> { reaction ->
                    assertThat(reaction.userId).isEqualTo("user-123")
                    assertThat(reaction.emoji).isEqualTo("\uD83D\uDD25")
                },
            )
        }
        verify(exactly = 1) { summaryReference.setValue(mapOf("\uD83D\uDD25" to 1)) }
        verify(exactly = 1) { countReference.setValue(1) }
    }

    @Test
    fun `setSnapshotReaction removes the active reaction when the same emoji is selected again`() = runTest {
        val currentUser = mockk<FirebaseUser>()
        every { currentUser.uid } returns "user-123"
        authSessionProvider.setCurrentUser(currentUser)

        val snapshotReference = mockk<DatabaseReference>()
        val reactionsReference = mockk<DatabaseReference>()
        val reactionUserReference = mockk<DatabaseReference>()
        val summaryReference = mockk<DatabaseReference>()
        val countReference = mockk<DatabaseReference>()
        val likeListReference = mockk<DataSnapshot>()
        val dateTimeSnapshot = mockk<DataSnapshot>()
        val reactionsSnapshot = mockk<DataSnapshot>()
        val snapshotState = mockk<DataSnapshot>()
        val existingReactionEntry = mockk<DataSnapshot>()

        every { snapshotsDatabase.child("snapshot-1") } returns snapshotReference
        every { snapshotReference.get() } returns Tasks.forResult(snapshotState)
        every { snapshotReference.child(Constants.REACTIONS_PATH) } returns reactionsReference
        every { reactionsReference.child("user-123") } returns reactionUserReference
        every { snapshotReference.child(Constants.REACTION_SUMMARY_PROPERTY) } returns summaryReference
        every { snapshotReference.child(Constants.REACTION_COUNT_PROPERTY) } returns countReference
        every { snapshotState.child("dateTime") } returns dateTimeSnapshot
        every { snapshotState.child(Constants.LIKE_LIST_PROPERTY) } returns likeListReference
        every { snapshotState.child(Constants.REACTIONS_PATH) } returns reactionsSnapshot
        every { dateTimeSnapshot.getValue(Long::class.java) } returns 100L
        every { likeListReference.children } returns emptyList()
        every { likeListReference.hasChild("user-123") } returns false
        every { reactionsSnapshot.children } returns listOf(existingReactionEntry)
        every { existingReactionEntry.getValue(SnapshotReactionDTO::class.java) } returns SnapshotReactionDTO(
            userId = "user-123",
            emoji = "\uD83D\uDD25",
            createdAt = 1L,
            updatedAt = 1L,
        )
        every { reactionUserReference.setValue(null) } returns Tasks.forResult(null)
        every { summaryReference.setValue(emptyMap<String, Int>()) } returns Tasks.forResult(null)
        every { countReference.setValue(0) } returns Tasks.forResult(null)

        val result = service.setSnapshotReaction("snapshot-1", "\uD83D\uDD25")

        assertThat(result).isEqualTo(1)
        verify(exactly = 1) { reactionUserReference.setValue(null) }
        verify(exactly = 1) { summaryReference.setValue(emptyMap<String, Int>()) }
        verify(exactly = 1) { countReference.setValue(0) }
    }

    @Test
    fun `addSnapshotReply stores the reply and updates replyCount`() = runTest {
        val snapshotReference = mockk<DatabaseReference>()
        val repliesReference = mockk<DatabaseReference>()
        val generatedReplyReference = mockk<DatabaseReference>()
        val storedReplyReference = mockk<DatabaseReference>()
        val replyCountReference = mockk<DatabaseReference>()
        val repliesSnapshot = mockk<DataSnapshot>()

        every { snapshotsDatabase.child("snapshot-1") } returns snapshotReference
        every { snapshotReference.child(Constants.REPLIES_PATH) } returns repliesReference
        every { repliesReference.push() } returns generatedReplyReference
        every { generatedReplyReference.key } returns "reply-1"
        every { repliesReference.child("reply-1") } returns storedReplyReference
        every { storedReplyReference.setValue(any()) } returns Tasks.forResult(null)
        every { repliesReference.get() } returns Tasks.forResult(repliesSnapshot)
        every { repliesSnapshot.childrenCount } returns 1L
        every { snapshotReference.child(Constants.REPLY_COUNT_PROPERTY) } returns replyCountReference
        every { replyCountReference.setValue(1) } returns Tasks.forResult(null)

        val reply = service.addSnapshotReply(
            snapshotId = "snapshot-1",
            reply = SnapshotReply(
                replyId = "local-1",
                snapshotId = "snapshot-1",
                idUserOwner = "user-123",
                userName = "Henry",
                userPhotoUrl = null,
                text = "Hello",
                dateTime = 100L,
                deliveryState = SnapshotReplyDeliveryState.PENDING,
            ),
        )

        assertThat(reply.replyId).isEqualTo("reply-1")
        assertThat(reply.deliveryState).isEqualTo(SnapshotReplyDeliveryState.CONFIRMED)
        verify(exactly = 1) {
            storedReplyReference.setValue(
                withArg<SnapshotReplyDTO> { dto ->
                    assertThat(dto.idUserOwner).isEqualTo("user-123")
                    assertThat(dto.userName).isEqualTo("Henry")
                    assertThat(dto.text).isEqualTo("Hello")
                },
            )
        }
        verify(exactly = 1) { replyCountReference.setValue(1) }
    }

    @Test
    fun `getSnapshotReplies enriches missing author photo from users data`() = runTest {
        val snapshotReference = mockk<DatabaseReference>()
        val repliesReference = mockk<DatabaseReference>()
        val repliesSnapshot = mockk<DataSnapshot>()
        val replyEntry = mockk<DataSnapshot>()
        val userSnapshot = mockk<DataSnapshot>()

        every { snapshotsDatabase.child("snapshot-1") } returns snapshotReference
        every { snapshotReference.child(Constants.REPLIES_PATH) } returns repliesReference
        every { repliesReference.get() } returns Tasks.forResult(repliesSnapshot)
        every { repliesSnapshot.children } returns listOf(replyEntry)
        every { replyEntry.key } returns "reply-1"
        every { replyEntry.getValue(SnapshotReplyDTO::class.java) } returns SnapshotReplyDTO(
            idUserOwner = "user-123",
            userName = "Henry",
            userPhotoUrl = "",
            text = "Hello",
            dateTime = 100L,
        )
        every { usersDatabase.child("user-123") } returns mockk<DatabaseReference>().also { userReference ->
            every { userReference.get() } returns Tasks.forResult(userSnapshot)
        }
        every { userSnapshot.getValue(com.hvasoft.dailydose.data.network.model.User::class.java) } returns
            com.hvasoft.dailydose.data.network.model.User(
                userName = "Henry",
                photoUrl = "https://example.com/profile.jpg",
            )

        val replies = service.getSnapshotReplies("snapshot-1")

        assertThat(replies).hasSize(1)
        assertThat(replies.single().userPhotoUrl).isEqualTo("https://example.com/profile.jpg")
    }

    @Test
    fun `deleteSnapshot uses the snapshot owner storage path`() = runTest {
        val ownerStorage = mockk<StorageReference>()
        val snapshotStorage = mockk<StorageReference>()
        val snapshotDatabase = mockk<DatabaseReference>()
        every { snapshotsRootStorage.child("owner-1") } returns ownerStorage
        every { ownerStorage.child("snapshot-1") } returns snapshotStorage
        every { snapshotsDatabase.child("snapshot-1") } returns snapshotDatabase
        every { snapshotStorage.delete() } returns Tasks.forResult<Void?>(null)
        every { snapshotDatabase.removeValue() } returns Tasks.forResult<Void?>(null)

        val result = service.deleteSnapshot(
            Snapshot(
                snapshotKey = "snapshot-1",
                idUserOwner = "owner-1",
            ),
        )

        assertThat(result).isEqualTo(1)
        verify(exactly = 1) { snapshotsRootStorage.child("owner-1") }
        verify(exactly = 1) { ownerStorage.child("snapshot-1") }
        verify(exactly = 1) { snapshotStorage.delete() }
        verify(exactly = 1) { snapshotDatabase.removeValue() }
    }
}
