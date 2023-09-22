package com.hvasoft.dailydose.data.network.data_source

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.StorageReference
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.domain.model.Snapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RemoteDatabaseServiceImpl @Inject constructor(
    private val snapshotsDatabase: DatabaseReference,
    private val usersDatabase: DatabaseReference,
    private val snapshotsStorage: StorageReference
) : RemoteDatabaseService {

//    override fun isServiceOnline(): Flow<Boolean> {
//        return callbackFlow {
//            val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")
//            val listener = object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    val isConnected = snapshot.getValue(Boolean::class.java) ?: false
//                    trySend(isConnected)
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    close(error.toException())
//                }
//            }
//            connectedRef.addValueEventListener(listener)
//
//            awaitClose {
//                connectedRef.removeEventListener(listener)
//            }
//        }
//    }

    override fun getSnapshots(): Flow<List<Snapshot>> {
        return callbackFlow {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val snapshotsWithKeys = snapshot.children
                            .mapNotNull { child ->
                                child.key?.let { key ->
                                    Pair(
                                        key,
                                        child.getValue<Snapshot>()
                                    )
                                }
                            }
                            .filter { it.second != null }
                            .map { pair ->
                                pair.second?.snapshotKey = pair.first
                                pair
                            }
                            .sortedByDescending { it.second?.dateTime }

                        val snapshots = snapshotsWithKeys.map { it.second!! }
                        trySend(snapshots)
                    } else {
                        throw Exception("Failed to load data")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }
            snapshotsDatabase.addValueEventListener(listener)

            awaitClose {
                snapshotsDatabase.removeEventListener(listener)
            }
        }
    }

    override fun getUserPhotoUrl(idUser: String?): Flow<String> {
        return callbackFlow {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userPhotoUrl = snapshot.getValue<String>()!!
                        trySend(userPhotoUrl)
                    } else {
                        throw Exception("Failed to load data")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }
            usersDatabase.child(idUser ?: "").child(Constants.PHOTO_URL_PATH)
                .addValueEventListener(listener)

            awaitClose {
                usersDatabase.removeEventListener(listener)
            }
        }
    }

    override fun getUserName(idUser: String?): Flow<String> {
        return callbackFlow {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userName = snapshot.getValue<String>()!!
                        trySend(userName)
                    } else {
                        throw Exception("Failed to load data")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }
            usersDatabase.child(idUser ?: "").child(Constants.USERNAME_PATH)
                .addValueEventListener(listener)

            awaitClose {
                usersDatabase.removeEventListener(listener)
            }
        }
    }

    override suspend fun toggleUserLike(snapshot: Snapshot, isChecked: Boolean): Int {
        val userLikeReference = snapshotsDatabase
            .child(snapshot.snapshotKey)
            .child(Constants.LIKE_LIST_PROPERTY)
            .child(Constants.currentUser.uid)
        return if (userLikeReference.key != null) {
            if (isChecked)
                userLikeReference.setValue(true)
            else
                userLikeReference.setValue(null)
            1
        } else 0
    }

    override suspend fun deleteSnapshot(snapshot: Snapshot): Int {
        if (snapshot.snapshotKey.isNotEmpty()) {
            try {
                withContext(Dispatchers.IO) {
                    snapshotsStorage.child(snapshot.snapshotKey).delete().await()
                    snapshotsDatabase.child(snapshot.snapshotKey).removeValue().await()
                }
                return 1
            } catch (exception: Exception) {
                throw Exception("Failed to delete snapshot", exception)
            }
        }
        return 0
    }
}