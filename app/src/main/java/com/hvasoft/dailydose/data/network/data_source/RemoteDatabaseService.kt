package com.hvasoft.dailydose.data.network.data_source

import com.hvasoft.dailydose.data.network.model.User
import com.hvasoft.dailydose.domain.model.CreateSnapshotResult
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.model.SnapshotReply
import kotlinx.coroutines.flow.Flow

interface RemoteDatabaseService {

    // TODO: Implementar error handling
//    fun isServiceOnline(): Flow<Boolean>
    fun getSnapshots(): Flow<List<Snapshot>>
    suspend fun getSnapshotsOnce(): List<Snapshot>
    fun getUserPhotoUrl(idUser: String?): Flow<String>
    suspend fun getUserPhotoUrlOnce(idUser: String?): String
    fun getUserName(idUser: String?): Flow<String>
    suspend fun getUserNameOnce(idUser: String?): String
    suspend fun getUsersOnce(userIds: Set<String>): Map<String, User>
    suspend fun setSnapshotReaction(snapshotId: String, emoji: String?): Int
    suspend fun getSnapshotReplies(snapshotId: String): List<SnapshotReply>
    suspend fun addSnapshotReply(snapshotId: String, reply: SnapshotReply): SnapshotReply
    suspend fun toggleUserLike(snapshot: Snapshot, isChecked: Boolean): Int
    suspend fun deleteSnapshot(snapshot: Snapshot): Int

    suspend fun publishSnapshot(
        localImageContentUri: String,
        title: String,
        onProgress: (Int) -> Unit,
    ): CreateSnapshotResult
}
