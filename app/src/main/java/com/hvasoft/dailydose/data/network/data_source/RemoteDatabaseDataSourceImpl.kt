package com.hvasoft.dailydose.data.network.data_source

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.StorageReference
import com.hvasoft.dailydose.data.utils.Constants
import com.hvasoft.dailydose.domain.model.Snapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class RemoteDatabaseDataSourceImpl @Inject constructor(
    private val snapshotsDatabase: DatabaseReference,
    private val usersDatabase: DatabaseReference,
    private val snapshotsStorage: StorageReference
) : RemoteDatabaseDataSource {

    override fun getSnapshots(): Flow<List<Snapshot>> {
        return callbackFlow {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val snapshots = snapshot.children
                        .mapNotNull { it.getValue<Snapshot>() }
                        .sortedByDescending { it.dateTime }

                    trySend(snapshots)
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
                        Log.d("hva_test", "getUserPhotoUrl: snapshot doesn't exist")
                        trySend("")
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
                        Log.d("hva_test", "getUserName: $userName")
                        trySend(userName)
                    } else {
                        Log.d("hva_test", "getUserName: snapshot doesn't exist")
                        trySend("")
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

}