package com.hvasoft.dailydose.data.network.data_source

import com.hvasoft.dailydose.domain.model.Snapshot
import kotlinx.coroutines.flow.Flow

interface RemoteDatabaseService {

    // TODO: Implementar error handling
//    fun isServiceOnline(): Flow<Boolean>
    fun getSnapshots(): Flow<List<Snapshot>>
    fun getUserPhotoUrl(idUser: String?): Flow<String>
    fun getUserName(idUser: String?): Flow<String>
    suspend fun toggleUserLike(snapshot: Snapshot, isChecked: Boolean): Int
    suspend fun deleteSnapshot(snapshot: Snapshot): Int

}