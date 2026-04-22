package com.hvasoft.dailydose.data.repository

import com.hvasoft.dailydose.data.local.CachedRevealStateDao
import com.hvasoft.dailydose.data.local.CachedRevealStateEntity
import com.hvasoft.dailydose.data.local.OfflineFeedItemDao
import com.hvasoft.dailydose.data.local.OfflineSnapshotReplyDao
import com.hvasoft.dailydose.data.local.PendingSnapshotActionEntity
import com.hvasoft.dailydose.data.local.PendingSnapshotActionDao
import com.hvasoft.dailydose.data.local.toOfflineEntity
import com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseService
import com.hvasoft.dailydose.domain.model.PendingSnapshotActionQueueState
import com.hvasoft.dailydose.domain.model.PendingSnapshotActionType
import com.hvasoft.dailydose.domain.model.SnapshotRevealSyncState
import com.hvasoft.dailydose.domain.model.SnapshotReply
import com.hvasoft.dailydose.domain.model.SnapshotReplyDeliveryState
import com.hvasoft.dailydose.domain.model.SnapshotVisibilityMode
import java.util.Base64
import javax.inject.Inject

class SnapshotInteractionSyncCoordinator @Inject constructor(
    private val remoteDatabaseService: RemoteDatabaseService,
    private val pendingSnapshotActionDao: PendingSnapshotActionDao,
    private val offlineFeedItemDao: OfflineFeedItemDao,
    private val offlineSnapshotReplyDao: OfflineSnapshotReplyDao,
    private val cachedRevealStateDao: CachedRevealStateDao,
) {

    suspend fun flushPendingActions(
        accountId: String,
        rollbackOnFailure: Boolean,
    ): SyncResult {
        val actions = pendingSnapshotActionDao.getByAccount(accountId)
            .filter { it.queueState != PendingSnapshotActionQueueState.DISCARDED }
            .sortedBy(PendingSnapshotActionEntity::createdAt)
        if (actions.isEmpty()) {
            return SyncResult()
        }

        var applied = false
        var rolledBack = false
        val affectedSnapshotIds = linkedSetOf<String>()

        actions.forEach { action ->
            affectedSnapshotIds += action.snapshotId
            val attemptTimestamp = System.currentTimeMillis()
            val nextAttemptCount = action.attemptCount + 1
            pendingSnapshotActionDao.updateState(
                actionId = action.actionId,
                queueState = PendingSnapshotActionQueueState.IN_FLIGHT,
                lastAttemptAt = attemptTimestamp,
                attemptCount = nextAttemptCount,
            )

            try {
                when (action.actionType) {
                    PendingSnapshotActionType.SET_REACTION -> {
                        remoteDatabaseService.setSnapshotReaction(
                            snapshotId = action.snapshotId,
                            emoji = decodeReactionEmoji(action.payload),
                        )
                    }

                    PendingSnapshotActionType.REMOVE_REACTION -> {
                        remoteDatabaseService.setSnapshotReaction(
                            snapshotId = action.snapshotId,
                            emoji = null,
                        )
                    }

                    PendingSnapshotActionType.ADD_REPLY -> {
                        val localReply = decodeReplyPayload(action.snapshotId, action.payload)
                        val confirmedReply = remoteDatabaseService.addSnapshotReply(
                            snapshotId = action.snapshotId,
                            reply = localReply,
                        )
                        offlineSnapshotReplyDao.deleteByReplyId(
                            accountId = action.accountId,
                            snapshotId = action.snapshotId,
                            replyId = localReply.replyId,
                        )
                        offlineSnapshotReplyDao.upsertAll(
                            listOf(confirmedReply.toOfflineEntity(action.accountId)),
                        )
                    }

                    PendingSnapshotActionType.MARK_REVEALED -> {
                        val revealedAt = decodeRevealTimestamp(action.payload)
                        remoteDatabaseService.markSnapshotRevealed(
                            snapshotId = action.snapshotId,
                            revealedAt = revealedAt,
                        )
                        cachedRevealStateDao.upsert(
                            CachedRevealStateEntity(
                                accountId = action.accountId,
                                snapshotId = action.snapshotId,
                                revealedAt = revealedAt,
                                syncState = SnapshotRevealSyncState.CONFIRMED,
                                updatedAt = System.currentTimeMillis(),
                            ),
                        )
                    }
                }
                pendingSnapshotActionDao.deleteById(action.actionId)
                applied = true
            } catch (_: Exception) {
                if (rollbackOnFailure && action.actionType != PendingSnapshotActionType.MARK_REVEALED) {
                    if (action.actionType == PendingSnapshotActionType.ADD_REPLY) {
                        val localReply = decodeReplyPayload(action.snapshotId, action.payload)
                        offlineSnapshotReplyDao.deleteByReplyId(
                            accountId = action.accountId,
                            snapshotId = action.snapshotId,
                            replyId = localReply.replyId,
                        )
                    }
                    pendingSnapshotActionDao.updateState(
                        actionId = action.actionId,
                        queueState = PendingSnapshotActionQueueState.FAILED,
                        lastAttemptAt = attemptTimestamp,
                        attemptCount = nextAttemptCount,
                    )
                    rolledBack = true
                } else {
                    pendingSnapshotActionDao.updateState(
                        actionId = action.actionId,
                        queueState = PendingSnapshotActionQueueState.QUEUED,
                        lastAttemptAt = attemptTimestamp,
                        attemptCount = nextAttemptCount,
                    )
                }
            }
        }

        affectedSnapshotIds.forEach { snapshotId ->
            refreshPendingFlags(accountId, snapshotId)
        }
        return SyncResult(applied = applied, rolledBack = rolledBack)
    }

    suspend fun refreshPendingFlags(accountId: String, snapshotId: String) {
        val cachedItem = offlineFeedItemDao.getBySnapshotId(accountId, snapshotId) ?: return
        val cachedRevealState = cachedRevealStateDao.getBySnapshotId(accountId, snapshotId)
        val actions = pendingSnapshotActionDao.getBySnapshot(accountId, snapshotId)
            .filter { it.queueState == PendingSnapshotActionQueueState.QUEUED || it.queueState == PendingSnapshotActionQueueState.IN_FLIGHT }
        val hasPendingReveal = actions.any { it.actionType == PendingSnapshotActionType.MARK_REVEALED }
        val isOwnerView = cachedItem.ownerUserId == accountId
        val isRevealedForViewer = isOwnerView || hasPendingReveal || cachedRevealState != null || cachedItem.isRevealedForViewer
        val revealSyncState = when {
            isOwnerView -> SnapshotRevealSyncState.CONFIRMED
            hasPendingReveal -> SnapshotRevealSyncState.PENDING
            cachedRevealState != null -> cachedRevealState.syncState
            else -> SnapshotRevealSyncState.NONE
        }
        val visibilityMode = when {
            isOwnerView -> SnapshotVisibilityMode.VISIBLE_OWNER
            isRevealedForViewer -> SnapshotVisibilityMode.VISIBLE_REVEALED
            cachedItem.visibilityMode == SnapshotVisibilityMode.HIDDEN_PENDING_STATE ->
                SnapshotVisibilityMode.HIDDEN_PENDING_STATE
            else -> SnapshotVisibilityMode.HIDDEN_UNREVEALED
        }
        offlineFeedItemDao.upsertAll(
            listOf(
                cachedItem.copy(
                    hasPendingReaction = actions.any {
                        it.actionType == PendingSnapshotActionType.SET_REACTION ||
                            it.actionType == PendingSnapshotActionType.REMOVE_REACTION
                    },
                    hasPendingReply = actions.any { it.actionType == PendingSnapshotActionType.ADD_REPLY },
                    visibilityMode = visibilityMode,
                    revealSyncState = revealSyncState,
                    isRevealedForViewer = isRevealedForViewer,
                ),
            ),
        )
    }

    data class SyncResult(
        val applied: Boolean = false,
        val rolledBack: Boolean = false,
    )

    companion object {
        fun encodeReactionPayload(emoji: String?): String =
            emoji?.takeIf(String::isNotBlank)?.let(::encodePart).orEmpty()

        fun decodeReactionEmoji(payload: String): String? =
            payload.takeIf(String::isNotBlank)?.let(::decodePart)?.takeIf(String::isNotBlank)

        fun encodeRevealPayload(revealedAt: Long): String = encodePart(revealedAt.toString())

        fun decodeRevealTimestamp(payload: String): Long =
            decodePart(payload).toLongOrNull() ?: 0L

        fun encodeReplyPayload(reply: SnapshotReply): String = listOf(
            reply.replyId,
            reply.idUserOwner,
            reply.userName,
            reply.userPhotoUrl.orEmpty(),
            reply.text,
            reply.dateTime.toString(),
        ).joinToString(separator = PAYLOAD_DELIMITER.toString(), transform = ::encodePart)

        fun decodeReplyPayload(snapshotId: String, payload: String): SnapshotReply {
            val parts = payload.split(PAYLOAD_DELIMITER)
            require(parts.size >= 6) { "Invalid reply payload" }
            return SnapshotReply(
                replyId = decodePart(parts[0]),
                snapshotId = snapshotId,
                idUserOwner = decodePart(parts[1]),
                userName = decodePart(parts[2]),
                userPhotoUrl = decodePart(parts[3]).takeIf(String::isNotBlank),
                text = decodePart(parts[4]),
                dateTime = decodePart(parts[5]).toLongOrNull() ?: 0L,
                deliveryState = SnapshotReplyDeliveryState.PENDING,
            )
        }

        private fun encodePart(value: String): String =
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.toByteArray(Charsets.UTF_8))

        private fun decodePart(value: String): String =
            String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)

        private const val PAYLOAD_DELIMITER = '|'
    }
}
