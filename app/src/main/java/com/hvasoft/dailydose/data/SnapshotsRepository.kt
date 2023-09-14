package com.hvasoft.dailydose.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import com.hvasoft.dailydose.data.model.Snapshot
import com.hvasoft.dailydose.data.network.RemoteDatabase

class SnapshotsRepository(
    private val remoteDatabase: RemoteDatabase = RemoteDatabase.getInstance()
) {

    val snapshots: LiveData<MutableList<Snapshot>?> = liveData {
        val snapshotsLiveData = remoteDatabase.getSnapshotsLiveData()
        emitSource(snapshotsLiveData.map { snapshots ->
            snapshots?.sortedByDescending { snapshot -> snapshot.dateTime }?.toMutableList()
        })
    }

    suspend fun refreshSnapshots() {
        remoteDatabase.refreshSnapshots()
    }

    suspend fun setLikeSnapshot(snapshot: Snapshot, checked: Boolean) {
        remoteDatabase.setLikeSnapshot(snapshot, checked)
    }

    suspend fun deleteSnapshot(snapshot: Snapshot, callback: (isSuccess: Boolean) -> Unit){
        callback (remoteDatabase.delete(snapshot))
    }
}