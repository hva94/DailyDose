package com.hvasoft.dailydose.data.network.data_source

import android.net.Uri
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.hvasoft.dailydose.data.auth.AuthSessionProvider
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.data.network.model.SnapshotDTO
import com.hvasoft.dailydose.data.network.model.User
import com.hvasoft.dailydose.domain.model.CreateSnapshotResult
import com.hvasoft.dailydose.domain.model.Snapshot
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    private val snapshotsRootStorage: StorageReference,
    private val authSessionProvider: AuthSessionProvider,
) : RemoteDatabaseService {

    override fun getSnapshots(): Flow<List<Snapshot>> {
        return callbackFlow {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val snapshotsWithKeys = snapshot.children
                            .mapNotNull { child ->
                                child.key?.let { key ->
                                    Pair(key, child.getValue(Snapshot::class.java))
                                }
                            }
                            .filter { it.second != null }
                            .map { pair ->
                                pair.second?.snapshotKey = pair.first
                                pair
                            }
                            .sortedByDescending { it.second?.dateTime }

                        trySend(snapshotsWithKeys.map { it.second!! })
                    } else {
                        trySend(emptyList())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }
            snapshotsDatabase.addValueEventListener(listener)
            awaitClose { snapshotsDatabase.removeEventListener(listener) }
        }
    }

    override suspend fun getSnapshotsOnce(): List<Snapshot> = withContext(Dispatchers.IO) {
        val snapshot = snapshotsDatabase.get().await()
        if (!snapshot.exists()) {
            return@withContext emptyList()
        }

        snapshot.children
            .mapNotNull { child ->
                child.key?.let { key ->
                    child.getValue(Snapshot::class.java)?.apply {
                        snapshotKey = key
                    }
                }
            }
            .sortedByDescending { it.dateTime }
    }

    override fun getUserPhotoUrl(idUser: String?): Flow<String> {
        return callbackFlow {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    trySend(if (snapshot.exists()) snapshot.getValue(String::class.java) ?: "" else "")
                }
                override fun onCancelled(error: DatabaseError) { close(error.toException()) }
            }
            usersDatabase.child(idUser ?: "").child(Constants.PHOTO_URL_PATH)
                .addValueEventListener(listener)
            awaitClose { usersDatabase.removeEventListener(listener) }
        }
    }

    override suspend fun getUserPhotoUrlOnce(idUser: String?): String = withContext(Dispatchers.IO) {
        usersDatabase.child(idUser ?: "").child(Constants.PHOTO_URL_PATH)
            .get()
            .await()
            .getValue(String::class.java)
            .orEmpty()
    }

    override fun getUserName(idUser: String?): Flow<String> {
        return callbackFlow {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    trySend(if (snapshot.exists()) snapshot.getValue(String::class.java) ?: "" else "")
                }
                override fun onCancelled(error: DatabaseError) { close(error.toException()) }
            }
            usersDatabase.child(idUser ?: "").child(Constants.USERNAME_PATH)
                .addValueEventListener(listener)
            awaitClose { usersDatabase.removeEventListener(listener) }
        }
    }

    override suspend fun getUserNameOnce(idUser: String?): String = withContext(Dispatchers.IO) {
        usersDatabase.child(idUser ?: "").child(Constants.USERNAME_PATH)
            .get()
            .await()
            .getValue(String::class.java)
            .orEmpty()
    }

    override suspend fun getUsersOnce(userIds: Set<String>): Map<String, User> = withContext(Dispatchers.IO) {
        val uniqueIds = userIds.filter(String::isNotBlank).toSet()
        if (uniqueIds.isEmpty()) {
            return@withContext emptyMap()
        }

        coroutineScope {
            uniqueIds.map { userId ->
                async {
                    val snapshot = usersDatabase.child(userId).get().await()
                    val user = snapshot.getValue(User::class.java) ?: User()
                    userId to user.apply { id = userId }
                }
            }.awaitAll().toMap()
        }
    }

    override suspend fun toggleUserLike(snapshot: Snapshot, isChecked: Boolean): Int {
        val currentUserId = authSessionProvider.requireCurrentUserId()
        val userLikeReference = snapshotsDatabase
            .child(snapshot.snapshotKey)
            .child(Constants.LIKE_LIST_PROPERTY)
            .child(currentUserId)
        return if (userLikeReference.key != null) {
            if (isChecked) userLikeReference.setValue(true)
            else userLikeReference.setValue(null)
            1
        } else 0
    }

    override suspend fun deleteSnapshot(snapshot: Snapshot): Int {
        if (snapshot.snapshotKey.isNotEmpty()) {
            val ownerUserId = snapshot.idUserOwner
                ?.takeIf(String::isNotBlank)
                ?: throw IllegalStateException("Snapshot owner is missing")
            try {
                withContext(Dispatchers.IO) {
                    snapshotsRootStorage.child(ownerUserId).child(snapshot.snapshotKey).delete().await()
                    snapshotsDatabase.child(snapshot.snapshotKey).removeValue().await()
                }
                return 1
            } catch (exception: Exception) {
                throw Exception("Failed to delete snapshot", exception)
            }
        }
        return 0
    }

    override suspend fun publishSnapshot(
        localImageContentUri: String,
        title: String,
        onProgress: (Int) -> Unit,
    ): CreateSnapshotResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authSessionProvider.currentUserSnapshotOrNull()
                ?: return@withContext CreateSnapshotResult.SaveFailed
            val userId = authSessionProvider.requireCurrentUserId()
            val uri = Uri.parse(localImageContentUri)
            val key = snapshotsDatabase.push().key
                ?: return@withContext CreateSnapshotResult.SaveFailed

            val uploadTask = snapshotsRootStorage.child(userId).child(key).putFile(uri)
            uploadTask.addOnProgressListener { taskSnapshot ->
                val total = taskSnapshot.totalByteCount
                onProgress(if (total > 0) (100 * taskSnapshot.bytesTransferred / total).toInt() else 0)
            }
            uploadTask.await()

            val downloadUri = uploadTask.snapshot.storage.downloadUrl.await()
            val dto = SnapshotDTO(
                idUserOwner = userId,
                title = title,
                dateTime = System.currentTimeMillis(),
                photoUrl = downloadUri.toString(),
            )
            snapshotsDatabase.child(key).setValue(dto).await()
            CreateSnapshotResult.Success(
                snapshot = Snapshot(
                    title = dto.title,
                    dateTime = dto.dateTime,
                    photoUrl = dto.photoUrl,
                    idUserOwner = dto.idUserOwner,
                    snapshotKey = key,
                    userName = currentUser.displayName,
                    userPhotoUrl = currentUser.photoUrl,
                    likeCount = "0",
                    syncedAt = dto.dateTime,
                ),
            )
        } catch (_: StorageException) {
            CreateSnapshotResult.ImageUploadFailed
        } catch (_: Exception) {
            CreateSnapshotResult.SaveFailed
        }
    }
}
