package com.hvasoft.dailydose.data.network.data_source

import com.hvasoft.dailydose.domain.model.Snapshot
import kotlinx.coroutines.flow.Flow

interface RemoteDatabaseDataSource {

    fun getSnapshots(): Flow<List<Snapshot>>
    fun getUserPhotoUrl(idUser: String?): Flow<String>

    fun getUserName(idUser: String?): Flow<String>

}