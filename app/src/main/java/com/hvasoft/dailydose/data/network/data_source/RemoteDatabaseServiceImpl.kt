package com.hvasoft.dailydose.data.network.data_source

import android.net.Uri
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.hvasoft.dailydose.data.auth.AuthSessionProvider
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.data.config.DailyPromptCatalogProvider
import com.hvasoft.dailydose.data.network.model.DailyPromptAssignmentDTO
import com.hvasoft.dailydose.data.network.model.SnapshotDTO
import com.hvasoft.dailydose.data.network.model.SnapshotRevealRecordDTO
import com.hvasoft.dailydose.data.network.model.SnapshotReactionDTO
import com.hvasoft.dailydose.data.network.model.SnapshotReplyDTO
import com.hvasoft.dailydose.data.network.model.User
import com.hvasoft.dailydose.data.network.model.UserPostingStatusDTO
import com.hvasoft.dailydose.domain.model.CreateSnapshotRequest
import com.hvasoft.dailydose.domain.model.CreateSnapshotResult
import com.hvasoft.dailydose.domain.model.DailyPromptAssignment
import com.hvasoft.dailydose.domain.model.DailyPromptComboSelector
import com.hvasoft.dailydose.domain.model.DailyPromptDay
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.model.SnapshotRevealSyncState
import com.hvasoft.dailydose.domain.model.SnapshotReply
import com.hvasoft.dailydose.domain.model.SnapshotReplyDeliveryState
import com.hvasoft.dailydose.domain.model.SnapshotTitleGenerationMode
import com.hvasoft.dailydose.domain.model.SnapshotVisibilityMode
import com.hvasoft.dailydose.domain.model.UserPostingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.logging.Logger
import javax.inject.Inject

class RemoteDatabaseServiceImpl @Inject constructor(
    private val snapshotsDatabase: DatabaseReference,
    private val usersDatabase: DatabaseReference,
    private val snapshotsRootStorage: StorageReference,
    private val authSessionProvider: AuthSessionProvider,
    private val dailyPromptCatalogProvider: DailyPromptCatalogProvider,
) : RemoteDatabaseService {

    override fun getSnapshots(): Flow<List<Snapshot>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentUserId = authSessionProvider.currentUserIdOrNull()
                val snapshots = if (snapshot.exists()) {
                    snapshot.children
                        .mapNotNull { child ->
                            runCatching { child.toDomainSnapshot(currentUserId) }
                                .onFailure { failure ->
                                    logger.warning(
                                        "Skipping malformed snapshot ${child.key.orEmpty()}: ${failure.message}",
                                    )
                                }
                                .getOrNull()
                        }
                        .sortedByDescending { it.dateTime }
                } else {
                    emptyList()
                }
                trySend(snapshots)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        snapshotsDatabase.addValueEventListener(listener)
        awaitClose { snapshotsDatabase.removeEventListener(listener) }
    }

    override suspend fun getSnapshotsOnce(): List<Snapshot> = withContext(Dispatchers.IO) {
        val currentUserId = authSessionProvider.currentUserIdOrNull()
        val snapshot = snapshotsDatabase.get().await()
        if (!snapshot.exists()) {
            return@withContext emptyList()
        }

        snapshot.children
            .mapNotNull { child ->
                runCatching { child.toDomainSnapshot(currentUserId) }
                    .onFailure { failure ->
                        logger.warning(
                            "Skipping malformed snapshot ${child.key.orEmpty()} during one-shot fetch: ${failure.message}",
                        )
                    }
                    .getOrNull()
            }
            .sortedByDescending { it.dateTime }
    }

    override fun getUserPhotoUrl(idUser: String?): Flow<String> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(if (snapshot.exists()) snapshot.getValue(String::class.java) ?: "" else "")
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        usersDatabase.child(idUser ?: "").child(Constants.PHOTO_URL_PATH)
            .addValueEventListener(listener)
        awaitClose { usersDatabase.removeEventListener(listener) }
    }

    override suspend fun getUserPhotoUrlOnce(idUser: String?): String = withContext(Dispatchers.IO) {
        usersDatabase.child(idUser ?: "").child(Constants.PHOTO_URL_PATH)
            .get()
            .await()
            .getValue(String::class.java)
            .orEmpty()
    }

    override fun getUserName(idUser: String?): Flow<String> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(if (snapshot.exists()) snapshot.getValue(String::class.java) ?: "" else "")
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        usersDatabase.child(idUser ?: "").child(Constants.USERNAME_PATH)
            .addValueEventListener(listener)
        awaitClose { usersDatabase.removeEventListener(listener) }
    }

    override suspend fun getUserNameOnce(idUser: String?): String = withContext(Dispatchers.IO) {
        usersDatabase.child(idUser ?: "").child(Constants.USERNAME_PATH)
            .get()
            .await()
            .getValue(String::class.java)
            .orEmpty()
    }

    override suspend fun getUsersOnce(userIds: Set<String>): Map<String, User> = withContext(Dispatchers.IO) {
        val uniqueIds = userIds.filter(String::isNotBlank).toSet()
        if (uniqueIds.isEmpty()) {
            return@withContext emptyMap()
        }

        coroutineScope {
            uniqueIds.map { userId ->
                async {
                    val snapshot = usersDatabase.child(userId).get().await()
                    val user = snapshot.getValue(User::class.java) ?: User()
                    userId to user.apply { id = userId }
                }
            }.awaitAll().toMap()
        }
    }

    override fun observeActiveDailyPrompt(): Flow<DailyPromptAssignment?> = callbackFlow {
        val dateKey = DailyPromptDay.currentDateKey()
        val promptReference = dailyPromptAssignmentsReference().child(dateKey)
        launch {
            trySend(resolveDailyPromptAssignment(dateKey))
        }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.toDailyPromptAssignment(dateKey))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        promptReference.addValueEventListener(listener)
        awaitClose { promptReference.removeEventListener(listener) }
    }

    override fun observeUserPostingStatus(): Flow<UserPostingStatus?> = callbackFlow {
        val userId = authSessionProvider.currentUserIdOrNull()
        if (userId.isNullOrBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val statusReference = usersDatabase.child(userId).child(Constants.USER_POSTING_STATUS_PATH)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.toUserPostingStatus(userId))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        statusReference.addValueEventListener(listener)
        awaitClose { statusReference.removeEventListener(listener) }
    }

    override suspend fun getRevealedSnapshots(snapshotIds: Set<String>): Map<String, Long> = withContext(Dispatchers.IO) {
        val userId = authSessionProvider.currentUserIdOrNull()
            ?.takeIf(String::isNotBlank)
            ?: return@withContext emptyMap()
        if (snapshotIds.isEmpty()) {
            return@withContext emptyMap()
        }

        val revealsSnapshot = usersDatabase.child(userId)
            .child(Constants.USER_REVEALED_SNAPSHOTS_PATH)
            .get()
            .await()
        if (!revealsSnapshot.exists()) {
            return@withContext emptyMap()
        }

        revealsSnapshot.children.mapNotNull { child ->
            val snapshotId = child.key?.takeIf(snapshotIds::contains) ?: return@mapNotNull null
            val revealedAt = child.toRevealTimestampOrNull() ?: return@mapNotNull null
            snapshotId to revealedAt
        }.toMap()
    }

    override suspend fun markSnapshotRevealed(snapshotId: String, revealedAt: Long) {
        withContext(Dispatchers.IO) {
            val userId = authSessionProvider.requireCurrentUserId()
            val revealReference = usersDatabase.child(userId)
                .child(Constants.USER_REVEALED_SNAPSHOTS_PATH)
                .child(snapshotId)
            val existingSnapshot = revealReference.get().await()
            val resolvedRevealedAt = existingSnapshot.toRevealTimestampOrNull() ?: revealedAt
            revealReference.setValue(
                SnapshotRevealRecordDTO(
                    revealedAt = resolvedRevealedAt,
                ),
            ).await()
        }
    }

    override suspend fun setSnapshotReaction(snapshotId: String, emoji: String?): Int = withContext(Dispatchers.IO) {
        val currentUserId = authSessionProvider.requireCurrentUserId()
        val snapshotReference = snapshotsDatabase.child(snapshotId)
        val snapshotState = snapshotReference.get().await()
        val existingReactions = snapshotState.buildLegacyAwareReactionMap()
        val now = System.currentTimeMillis()
        val updatedReactions = existingReactions.toMutableMap()
        val sanitizedEmoji = emoji?.takeIf(String::isNotBlank)
        val resolvedEmoji = when {
            sanitizedEmoji == null -> null
            updatedReactions[currentUserId]?.emoji == sanitizedEmoji -> null
            else -> sanitizedEmoji
        }

        if (resolvedEmoji == null) {
            snapshotReference.child(Constants.REACTIONS_PATH).child(currentUserId).setValue(null).await()
            updatedReactions.remove(currentUserId)
        } else {
            val updatedReaction = SnapshotReactionDTO(
                userId = currentUserId,
                emoji = resolvedEmoji,
                createdAt = updatedReactions[currentUserId]?.createdAt ?: now,
                updatedAt = now,
            )
            snapshotReference.child(Constants.REACTIONS_PATH).child(currentUserId)
                .setValue(updatedReaction)
                .await()
            updatedReactions[currentUserId] = updatedReaction
        }

        if (snapshotState.child(Constants.LIKE_LIST_PROPERTY).hasChild(currentUserId)) {
            snapshotReference.child(Constants.LIKE_LIST_PROPERTY).child(currentUserId).setValue(null).await()
        }

        val summary = updatedReactions.values
            .groupingBy(SnapshotReactionDTO::emoji)
            .eachCount()
            .filterValues { it > 0 }
        snapshotReference.child(Constants.REACTION_SUMMARY_PROPERTY).setValue(summary).await()
        snapshotReference.child(Constants.REACTION_COUNT_PROPERTY).setValue(updatedReactions.size).await()
        1
    }

    override suspend fun getSnapshotReplies(snapshotId: String): List<SnapshotReply> = withContext(Dispatchers.IO) {
        val repliesSnapshot = snapshotsDatabase.child(snapshotId).child(Constants.REPLIES_PATH).get().await()
        val replies = repliesSnapshot.children
            .mapNotNull { child ->
                val dto = child.getValue(SnapshotReplyDTO::class.java) ?: return@mapNotNull null
                SnapshotReply(
                    replyId = child.key.orEmpty(),
                    snapshotId = snapshotId,
                    idUserOwner = dto.idUserOwner,
                    userName = dto.userName.ifBlank { "Unknown user" },
                    userPhotoUrl = dto.userPhotoUrl.takeIf(String::isNotBlank),
                    text = dto.text,
                    dateTime = dto.dateTime,
                    deliveryState = SnapshotReplyDeliveryState.CONFIRMED,
                )
            }
        val missingProfileUserIds = replies
            .filter { it.userName.isBlank() || it.userPhotoUrl.isNullOrBlank() }
            .map(SnapshotReply::idUserOwner)
            .filter(String::isNotBlank)
            .toSet()
        val usersById = if (missingProfileUserIds.isEmpty()) {
            emptyMap()
        } else {
            runCatching { getUsersOnce(missingProfileUserIds) }
                .getOrElse {
                    logger.warning(
                        "Failed to enrich reply authors for snapshot $snapshotId: ${it.message}",
                    )
                    emptyMap()
                }
        }

        replies.map { reply ->
            val user = usersById[reply.idUserOwner]
            reply.copy(
                userName = reply.userName.ifBlank { user?.userName.orEmpty() }.ifBlank { "Unknown user" },
                userPhotoUrl = reply.userPhotoUrl
                    ?.takeIf(String::isNotBlank)
                    ?: user?.photoUrl?.takeIf(String::isNotBlank),
            )
        }.sortedWith(compareBy<SnapshotReply> { it.dateTime }.thenBy { it.replyId })
    }

    override suspend fun addSnapshotReply(snapshotId: String, reply: SnapshotReply): SnapshotReply = withContext(Dispatchers.IO) {
        val snapshotReference = snapshotsDatabase.child(snapshotId)
        val repliesReference = snapshotReference.child(Constants.REPLIES_PATH)
        val replyId = repliesReference.push().key
            ?: throw IllegalStateException("Failed to create reply id")

        val dto = SnapshotReplyDTO(
            idUserOwner = reply.idUserOwner,
            userName = reply.userName,
            userPhotoUrl = reply.userPhotoUrl.orEmpty(),
            text = reply.text,
            dateTime = reply.dateTime,
        )
        repliesReference.child(replyId).setValue(dto).await()
        val replyCount = repliesReference.get().await().childrenCount.toInt()
        snapshotReference.child(Constants.REPLY_COUNT_PROPERTY).setValue(replyCount).await()
        reply.copy(
            replyId = replyId,
            deliveryState = SnapshotReplyDeliveryState.CONFIRMED,
        )
    }

    override suspend fun toggleUserLike(snapshot: Snapshot, isChecked: Boolean): Int =
        setSnapshotReaction(
            snapshotId = snapshot.snapshotKey,
            emoji = if (isChecked) Constants.DEFAULT_HEART_REACTION else null,
        )

    override suspend fun deleteSnapshot(snapshot: Snapshot): Int {
        if (snapshot.snapshotKey.isNotEmpty()) {
            val ownerUserId = snapshot.idUserOwner
                ?.takeIf(String::isNotBlank)
                ?: throw IllegalStateException("Snapshot owner is missing")
            try {
                withContext(Dispatchers.IO) {
                    snapshotsRootStorage.child(ownerUserId).child(snapshot.snapshotKey).delete().await()
                    snapshotsDatabase.child(snapshot.snapshotKey).removeValue().await()
                }
                return 1
            } catch (exception: Exception) {
                throw Exception("Failed to delete snapshot", exception)
            }
        }
        return 0
    }

    override suspend fun publishSnapshot(
        request: CreateSnapshotRequest,
        onProgress: (Int) -> Unit,
    ): CreateSnapshotResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authSessionProvider.currentUserSnapshotOrNull()
                ?: return@withContext CreateSnapshotResult.SaveFailed
            val userId = authSessionProvider.requireCurrentUserId()
            val uri = Uri.parse(request.localImageContentUri)
            val key = snapshotsDatabase.push().key
                ?: return@withContext CreateSnapshotResult.SaveFailed

            val uploadTask = snapshotsRootStorage.child(userId).child(key).putFile(uri)
            uploadTask.addOnProgressListener { taskSnapshot ->
                val total = taskSnapshot.totalByteCount
                onProgress(if (total > 0) (100 * taskSnapshot.bytesTransferred / total).toInt() else 0)
            }
            uploadTask.await()

            val downloadUri = uploadTask.snapshot.storage.downloadUrl.await()
            val publishedAt = System.currentTimeMillis()
            val dto = SnapshotDTO(
                idUserOwner = userId,
                title = request.title,
                dateTime = publishedAt,
                photoUrl = downloadUri.toString(),
                dailyPromptId = request.dailyPromptId,
                dailyPromptText = request.dailyPromptText,
                titleGenerationMode = request.titleGenerationMode.name,
                reactionCount = 0,
                reactionSummary = emptyMap(),
                replyCount = 0,
            )
            snapshotsDatabase.child(key).setValue(dto).await()
            usersDatabase.child(userId)
                .child(Constants.USER_POSTING_STATUS_PATH)
                .setValue(
                    UserPostingStatusDTO(
                        lastPostedAt = publishedAt,
                        lastPromptComboId = request.dailyPromptId,
                    ),
                )
                .await()
            CreateSnapshotResult.Success(
                snapshot = Snapshot(
                    title = dto.title,
                    dateTime = dto.dateTime,
                    photoUrl = dto.photoUrl,
                    idUserOwner = dto.idUserOwner,
                    dailyPromptId = dto.dailyPromptId,
                    dailyPromptText = dto.dailyPromptText,
                    titleGenerationMode = dto.titleGenerationMode
                        .let(SnapshotTitleGenerationMode::valueOf),
                    snapshotKey = key,
                    userName = currentUser.displayName,
                    userPhotoUrl = currentUser.photoUrl,
                    reactionCount = 0,
                    reactionSummary = emptyMap(),
                    replyCount = 0,
                    visibilityMode = SnapshotVisibilityMode.VISIBLE_OWNER,
                    revealSyncState = SnapshotRevealSyncState.CONFIRMED,
                    isRevealedForViewer = true,
                    isOwnerView = true,
                    likeCount = "0",
                    syncedAt = dto.dateTime,
                ),
            )
        } catch (_: StorageException) {
            CreateSnapshotResult.ImageUploadFailed
        } catch (_: Exception) {
            CreateSnapshotResult.SaveFailed
        }
    }

    private fun DataSnapshot.toDomainSnapshot(currentUserId: String?): Snapshot? {
        val snapshotKey = key ?: return null
        val title = child("title").getValue(String::class.java).orEmpty()
        val dateTime = child("dateTime").getValue(Long::class.java) ?: 0L
        val photoUrl = child("photoUrl").getValue(String::class.java).orEmpty()
        val idUserOwner = child("idUserOwner").getValue(String::class.java).orEmpty()
        val dailyPromptId = child(Constants.DAILY_PROMPT_ID_PROPERTY).getValue(String::class.java)
            ?.takeIf(String::isNotBlank)
        val dailyPromptText = child(Constants.DAILY_PROMPT_TEXT_PROPERTY).getValue(String::class.java)
            ?.takeIf(String::isNotBlank)
        val titleGenerationMode = child(Constants.TITLE_GENERATION_MODE_PROPERTY)
            .getValue(String::class.java)
            ?.let {
                runCatching { SnapshotTitleGenerationMode.valueOf(it) }
                    .getOrDefault(SnapshotTitleGenerationMode.NONE)
            }
            ?: SnapshotTitleGenerationMode.NONE
        val likeList = child(Constants.LIKE_LIST_PROPERTY).children
            .mapNotNull { likeEntry ->
                likeEntry.key?.let { userId ->
                    userId to (likeEntry.getValue(Boolean::class.java) ?: true)
                }
            }
            .toMap()
        val storedReactionSummary = child(Constants.REACTION_SUMMARY_PROPERTY).children
            .mapNotNull { summaryEntry ->
                val emoji = summaryEntry.key ?: return@mapNotNull null
                val count = summaryEntry.getValue(Long::class.java)?.toInt()
                    ?: summaryEntry.getValue(Int::class.java)
                    ?: return@mapNotNull null
                emoji to count
            }
            .toMap()
        val storedReactionCount = child(Constants.REACTION_COUNT_PROPERTY).getValue(Long::class.java)?.toInt()
            ?: child(Constants.REACTION_COUNT_PROPERTY).getValue(Int::class.java)
            ?: 0
        val storedReplyCount = child(Constants.REPLY_COUNT_PROPERTY).getValue(Long::class.java)?.toInt()
            ?: child(Constants.REPLY_COUNT_PROPERTY).getValue(Int::class.java)
            ?: 0
        val reactions = buildLegacyAwareReactionMap()
        val derivedSummary = reactions.values.groupingBy(SnapshotReactionDTO::emoji).eachCount()
        val resolvedSummary = when {
            storedReactionSummary.isNotEmpty() -> storedReactionSummary.filterValues { it > 0 }
            derivedSummary.isNotEmpty() -> derivedSummary
            likeList.isNotEmpty() -> mapOf(Constants.DEFAULT_HEART_REACTION to likeList.size)
            else -> emptyMap()
        }
        val resolvedReactionCount = when {
            storedReactionCount > 0 -> storedReactionCount
            resolvedSummary.isNotEmpty() -> resolvedSummary.values.sum()
            else -> likeList.size
        }
        val currentUserReaction = currentUserId?.let { userId -> reactions[userId]?.emoji }
        val derivedReplyCount = child(Constants.REPLIES_PATH).childrenCount.toInt()

        return Snapshot(
            title = title,
            dateTime = dateTime,
            photoUrl = photoUrl,
            likeList = likeList,
            idUserOwner = idUserOwner,
            dailyPromptId = dailyPromptId,
            dailyPromptText = dailyPromptText,
            titleGenerationMode = titleGenerationMode,
            reactionCount = resolvedReactionCount,
            reactionSummary = resolvedSummary,
            replyCount = maxOf(storedReplyCount, derivedReplyCount),
            currentUserReaction = currentUserReaction,
            legacyLikeCount = likeList.size.takeIf { it > 0 },
            visibilityMode = if (idUserOwner == currentUserId && currentUserId.isNullOrBlank().not()) {
                SnapshotVisibilityMode.VISIBLE_OWNER
            } else {
                SnapshotVisibilityMode.HIDDEN_UNREVEALED
            },
            revealSyncState = if (idUserOwner == currentUserId && currentUserId.isNullOrBlank().not()) {
                SnapshotRevealSyncState.CONFIRMED
            } else {
                SnapshotRevealSyncState.NONE
            },
            isRevealedForViewer = idUserOwner == currentUserId && currentUserId.isNullOrBlank().not(),
            isOwnerView = idUserOwner == currentUserId && currentUserId.isNullOrBlank().not(),
            snapshotKey = snapshotKey,
            isLikedByCurrentUser = currentUserReaction != null,
            likeCount = resolvedReactionCount.toString(),
        )
    }

    private fun DataSnapshot.buildLegacyAwareReactionMap(): Map<String, SnapshotReactionDTO> {
        val migratedReactions = linkedMapOf<String, SnapshotReactionDTO>()
        val fallbackCreatedAt = child("dateTime").getValue(Long::class.java) ?: 0L

        child(Constants.LIKE_LIST_PROPERTY).children.forEach { likeEntry ->
            val userId = likeEntry.key ?: return@forEach
            migratedReactions[userId] = SnapshotReactionDTO(
                userId = userId,
                emoji = Constants.DEFAULT_HEART_REACTION,
                createdAt = fallbackCreatedAt,
                updatedAt = fallbackCreatedAt,
            )
        }

        child(Constants.REACTIONS_PATH).children.forEach { reactionEntry ->
            val dto = reactionEntry.getValue(SnapshotReactionDTO::class.java) ?: SnapshotReactionDTO()
            val userId = dto.userId.ifBlank { reactionEntry.key.orEmpty() }
            if (userId.isBlank()) return@forEach
            migratedReactions[userId] = dto.copy(userId = userId)
        }
        return migratedReactions
    }

    private suspend fun resolveDailyPromptAssignment(dateKey: String): DailyPromptAssignment? {
        val promptReference = dailyPromptAssignmentsReference().child(dateKey)
        val existingAssignment = promptReference.get().await().toDailyPromptAssignment(dateKey)
        if (existingAssignment != null) return existingAssignment

        val previousDateKey = DailyPromptDay.previousDateKey(dateKey)
        val previousPromptSnapshot = dailyPromptAssignmentsReference()
            .child(previousDateKey)
            .get()
            .await()
        val previousAssignment = previousPromptSnapshot.toDailyPromptAssignment(previousDateKey)
        val previousComboId = previousAssignment?.comboId ?: previousPromptSnapshot.toStoredPromptComboId()
        val combo = DailyPromptComboSelector.resolveForDay(
            combos = dailyPromptCatalogProvider.getDailyPromptCombos(),
            dateKey = dateKey,
            previousComboId = previousComboId,
        ) ?: return null
        val dto = DailyPromptAssignmentDTO(
            comboId = combo.comboId,
            promptText = combo.promptText,
            titlePatterns = combo.titlePatterns,
            answerFormats = combo.answerFormats,
            assignedAt = System.currentTimeMillis(),
            previousComboId = previousComboId,
        )
        promptReference.setValue(dto).await()
        return dto.toDomain(dateKey)
    }

    private fun dailyPromptAssignmentsReference(): DatabaseReference =
        snapshotsDatabase.root.child(Constants.DAILY_PROMPT_ASSIGNMENTS_PATH)

    private fun DataSnapshot.toDailyPromptAssignment(dateKey: String): DailyPromptAssignment? {
        val dto = getValue(DailyPromptAssignmentDTO::class.java) ?: return null
        return dto.toDomain(dateKey)
    }

    private fun DataSnapshot.toStoredPromptComboId(): String? =
        getValue(DailyPromptAssignmentDTO::class.java)
            ?.comboId
            ?.takeIf(String::isNotBlank)

    private fun DailyPromptAssignmentDTO.toDomain(dateKey: String): DailyPromptAssignment? {
        val sanitizedTitlePatterns = titlePatterns
            .map(String::trim)
            .filter(String::isNotBlank)
        val sanitizedAnswerFormats = answerFormats
            .map(String::trim)
            .filter(String::isNotBlank)
        if (
            comboId.isBlank() ||
            promptText.isBlank() ||
            sanitizedTitlePatterns.isEmpty() ||
            sanitizedAnswerFormats.isEmpty()
        ) {
            return null
        }
        return DailyPromptAssignment(
            dateKey = dateKey,
            comboId = comboId,
            promptText = promptText,
            titlePatterns = sanitizedTitlePatterns,
            answerFormats = sanitizedAnswerFormats,
            assignedAt = assignedAt,
            previousComboId = previousComboId,
        )
    }

    private fun DataSnapshot.toUserPostingStatus(userId: String): UserPostingStatus? {
        val dto = getValue(UserPostingStatusDTO::class.java) ?: return null
        return UserPostingStatus(
            userId = userId,
            lastPostedAt = dto.lastPostedAt,
            lastPromptComboId = dto.lastPromptComboId,
        )
    }

    private fun DataSnapshot.toRevealTimestampOrNull(): Long? =
        getValue(SnapshotRevealRecordDTO::class.java)
            ?.revealedAt
            ?.takeIf { it > 0 }
            ?: child(Constants.REVEALED_AT_PROPERTY).getValue(Long::class.java)
            ?.takeIf { it > 0 }
            ?: getValue(Long::class.java)
            ?.takeIf { it > 0 }

    private companion object {
        val logger: Logger = Logger.getLogger(RemoteDatabaseServiceImpl::class.java.name)
    }
}
