package com.hvasoft.dailydose.data.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.utils.DataConstants
import com.hvasoft.dailydose.data.model.Response
import com.hvasoft.dailydose.data.model.Snapshot
import kotlinx.coroutines.tasks.await

class RemoteDatabase(
    private val snapshotsDBRef: DatabaseReference = FirebaseDatabase.getInstance().reference
        .child(DataConstants.SNAPSHOTS_PATH),
    private val usersDBRef: DatabaseReference = FirebaseDatabase.getInstance().reference
        .child(DataConstants.USERS_PATH),
    private val snapshotsStorageRef: StorageReference = FirebaseStorage.getInstance().reference
        .child(DataConstants.SNAPSHOTS_PATH).child(DataConstants.currentUser.uid)
) {
    companion object {
        private var INSTANCE: RemoteDatabase? = null

        fun getInstance() = INSTANCE ?: synchronized(this) {
            RemoteDatabase().also { INSTANCE = it }
        }
    }

    private val snapshots = mutableListOf<Snapshot>()
    private val snapshotsLiveData: MutableLiveData<MutableList<Snapshot>?> = MutableLiveData()

    suspend fun getSnapshotsLiveData(): LiveData<MutableList<Snapshot>?> {
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
        snapshotsLiveData.value = resultList
        return snapshotsLiveData
    }

    suspend fun refreshSnapshots() {
        snapshots.clear()
        snapshotsLiveData.postValue(null)
        getSnapshotsLiveData()
    }

    suspend fun delete(snapshot: Snapshot): Boolean {
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

    suspend fun setLikeSnapshot(snapshot: Snapshot, checked: Boolean) {
        val snapshotRef = snapshotsDBRef.child(snapshot.id)
            .child(DataConstants.LIKE_LIST_PROPERTY)
            .child(DataConstants.currentUser.uid)

        if (checked) snapshotRef.setValue(checked)
        else snapshotRef.setValue(null)

        getSnapshotsLiveData()
    }
}