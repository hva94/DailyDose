package com.hvasoft.dailydose.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseDataSource
import com.hvasoft.dailydose.data.paging.SnapshotPagingSource
import com.hvasoft.dailydose.data.utils.Constants
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.repository.HomeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val remoteDatabaseDataSource: RemoteDatabaseDataSource
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

//    override suspend fun getPagedSnapshots(): Resource<List<Snapshot>> {
//        return try {
////                val response: Response<SnapshotResponse> = remoteDatabaseApi.getSnapshots()
////                if (response.isSuccessful) {
////                    val snapshots = response.body()?.snapshots
////                        ?.sortedByDescending { snapshot -> snapshot.dateTime }
////                        ?.toMutableList()
////                    emit(Resource.success(snapshots))
////                } else {
////                    emit(Resource.error(response.message(), null))
////                }
//                val snapshots = withContext(Dispatchers.IO) {
//                    remoteDatabaseApi.getSnapshots()
//                }
//                    .sortedByDescending { snapshot -> snapshot.dateTime }
//                    .toMutableList()
//                if (snapshots.isNotEmpty()) {
//                    Resource.success(snapshots)
//                } else {
//                    Resource.error("TODO - Network problem - TODO", null)
//                }
//            } catch (e: Throwable) {
//                Resource.error(e.cause?.localizedMessage ?: "An error occurred", null)
//            }
//        }
//    }

    override suspend fun getPagedSnapshots(): Flow<PagingData<Snapshot>> {
        return Pager(
            config = PagingConfig(pageSize = Constants.SNAPSHOTS_ITEMS_PER_PAGE),
            pagingSourceFactory = { SnapshotPagingSource(remoteDatabaseDataSource) }
        ).flow
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
}