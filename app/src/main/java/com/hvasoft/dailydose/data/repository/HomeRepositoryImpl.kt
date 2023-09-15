package com.hvasoft.dailydose.data.repository

import android.util.Log
import com.hvasoft.dailydose.data.model.Snapshot
import com.hvasoft.dailydose.data.network.RemoteDatabaseService
import com.hvasoft.dailydose.domain.HomeRepository
import com.hvasoft.dailydose.domain.common.response_handling.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val remoteDatabaseService: RemoteDatabaseService
) : HomeRepository {

    override suspend fun getSnapshots(): Result<List<Snapshot>> {
        return try {
            val snapshots = withContext(Dispatchers.IO) {
                remoteDatabaseService.getSnapshots()
            }
                .sortedByDescending { snapshot -> snapshot.dateTime }
                .toMutableList()

            Log.d("hva_test", "getSnapshots: ${snapshots.size}")
            Result.Success(snapshots)
        } catch (e: Exception) {
            Log.d("hva_test", "Exception: ${e.message}")
            Result.Exception(e)
        }
    }

    override suspend fun isLikeChanged(snapshot: Snapshot, isLiked: Boolean): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                remoteDatabaseService.setLikeSnapshot(snapshot, isLiked)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun isSnapshotDeleted(snapshot: Snapshot): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                remoteDatabaseService.deleteSnapshot(snapshot)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}