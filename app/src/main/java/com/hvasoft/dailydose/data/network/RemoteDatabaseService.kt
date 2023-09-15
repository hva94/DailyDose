package com.hvasoft.dailydose.data.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.model.Response
import com.hvasoft.dailydose.data.model.Snapshot
import com.hvasoft.dailydose.data.utils.DataConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RemoteDatabaseService(
    private val snapshotsDBRef: DatabaseReference = FirebaseDatabase.getInstance().reference
        .child(DataConstants.SNAPSHOTS_PATH),
    private val usersDBRef: DatabaseReference = FirebaseDatabase.getInstance().reference
        .child(DataConstants.USERS_PATH),
    private val snapshotsStorageRef: StorageReference = FirebaseStorage.getInstance().reference
        .child(DataConstants.SNAPSHOTS_PATH).child(DataConstants.currentUser.uid)
) {
//    companion object {
//        private var INSTANCE: RemoteDatabaseService? = null
//
//        fun getInstance() = INSTANCE ?: synchronized(this) {
//            RemoteDatabaseService().also { INSTANCE = it }
//        }
//    }

    private val snapshots = mutableListOf<Snapshot>()
    private val snapshotsLiveData: MutableLiveData<MutableList<Snapshot>?> = MutableLiveData()

    private suspend fun <T> LiveData<T>.awaitValue(): T? {
        return withContext(Dispatchers.Main) {
            var value: T? = null
            val observer = Observer<T> {
                value = it
            }
            observeForever(observer)
            removeObserver(observer)
            return@withContext value
        }
    }

    suspend fun getSnapshots(): List<Snapshot> {
        val liveDataSnapshots: MutableList<Snapshot>? = getSnapshotsLiveData().awaitValue()
        return liveDataSnapshots?.toList() ?: emptyList()
    }

    private suspend fun getSnapshotsLiveData(): LiveData<MutableList<Snapshot>?> {
        if (snapshots.size == 0) {
            val response = Response()
            try {
                response.snapshots = snapshotsDBRef.get().await().children.map { snapshotData ->
                    val snapshot = snapshotData.getValue(Snapshot::class.java)!!
                    snapshot.id = snapshotData.key.toString()

                    usersDBRef.child(snapshot.idUserOwner).get().await().let { userData ->
                        if (userData.exists()) {
                            snapshot.userName = userData.child("userName").value.toString()
                            snapshot.userPhotoUrl = userData.child("photoUrl").value.toString()
                        } else {
                            snapshot.userName = R.string.home_not_found_user_error.toString()
                        }
                    }

                    snapshots.add(snapshot)
                    snapshot
                }
            } catch (exception: Exception) {
                response.exception = exception
            }
        }

        val resultList = mutableListOf<Snapshot>()
        resultList.addAll(snapshots)
        snapshotsLiveData.postValue(resultList)
        return snapshotsLiveData
    }

//    suspend fun refreshSnapshots(): List<Snapshot> {
//        snapshots.clear()
//        snapshotsLiveData.postValue(null)
//
//        val liveDataSnapshots: MutableList<Snapshot>? = getSnapshotsLiveData().awaitValue()
//        return liveDataSnapshots?.toList() ?: emptyList()
//    }

    suspend fun deleteSnapshot(snapshot: Snapshot): Boolean {
        val index = snapshots.indexOf(snapshot)

        if (index != -1) {
            if (snapshots.removeAt(index) == snapshot) {

                snapshotsStorageRef
                    .child(snapshot.id)
                    .delete().addOnCompleteListener { result ->
                        if (result.isSuccessful) {
                            snapshotsDBRef.child(snapshot.id).removeValue()
                        }
                    }.await().also { getSnapshotsLiveData() }

                return true
            }
        }
        return false
    }

    suspend fun setLikeSnapshot(snapshot: Snapshot, checked: Boolean): List<Snapshot> {
        val snapshotRef = snapshotsDBRef.child(snapshot.id)
            .child(DataConstants.LIKE_LIST_PROPERTY)
            .child(DataConstants.currentUser.uid)

        if (checked) snapshotRef.setValue(checked)
        else snapshotRef.setValue(null)

        val liveDataSnapshots: MutableList<Snapshot>? = getSnapshotsLiveData().awaitValue()
        return liveDataSnapshots?.toList() ?: emptyList()
    }
}