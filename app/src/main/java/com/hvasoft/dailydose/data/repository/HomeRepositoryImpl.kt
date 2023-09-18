package com.hvasoft.dailydose.data.repository

import com.hvasoft.dailydose.data.network.RemoteDatabaseApi
import com.hvasoft.dailydose.data.network.model.Snapshot
import com.hvasoft.dailydose.domain.common.response_handling.Resource
import com.hvasoft.dailydose.domain.repository.HomeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val remoteDatabaseApi: RemoteDatabaseApi
) : HomeRepository {

//    override suspend fun getSnapshots(): Result<List<Snapshot>> {
//        return try {
//            val snapshots = withContext(Dispatchers.IO) {
//                remoteDatabaseService.getSnapshots()
//            }
//                .sortedByDescending { snapshot -> snapshot.dateTime }
//                .toMutableList()
//
////            Log.d("hva_test", "getSnapshots: ${snapshots.size}")
//            Result.Success(snapshots)
//        } catch (e: Exception) {
////            Log.d("hva_test", "Exception: ${e.message}")
//            Result.Exception(e)
//        }
//    }

    override suspend fun getSnapshots(): Resource<List<Snapshot>> {
        return try {
//                val response: Response<SnapshotResponse> = remoteDatabaseApi.getSnapshots()
//                if (response.isSuccessful) {
//                    val snapshots = response.body()?.snapshots
//                        ?.sortedByDescending { snapshot -> snapshot.dateTime }
//                        ?.toMutableList()
//                    emit(Resource.success(snapshots))
//                } else {
//                    emit(Resource.error(response.message(), null))
//                }
                val snapshots = withContext(Dispatchers.IO) {
                    remoteDatabaseApi.getSnapshots()
                }
                    .sortedByDescending { snapshot -> snapshot.dateTime }
                    .toMutableList()
                if (snapshots.isNotEmpty()) {
                    Resource.success(snapshots)
                } else {
                    Resource.error("TEMPORARY - Network problem - TEMPORARY", null)
                }
            } catch (e: Throwable) {
                Resource.error(e.cause?.localizedMessage ?: "An error occurred", null)
            }
        }
    }

//    override suspend fun isLikeChanged(snapshot: Snapshot, isLiked: Boolean): Boolean {
//        return try {
//            withContext(Dispatchers.IO) {
//                remoteDatabaseApi.setLikeSnapshot(snapshot, isLiked)
//            }
//            true
//        } catch (e: Exception) {
//            false
//        }
//    }
//
//    override suspend fun isSnapshotDeleted(snapshot: Snapshot): Boolean {
//        return try {
//            withContext(Dispatchers.IO) {
//                remoteDatabaseApi.deleteSnapshot(snapshot)
//            }
//            true
//        } catch (e: Exception) {
//            false
//        }
//    }
//}