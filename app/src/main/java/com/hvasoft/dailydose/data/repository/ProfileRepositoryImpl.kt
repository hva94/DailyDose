package com.hvasoft.dailydose.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import com.hvasoft.dailydose.data.network.model.User
import com.hvasoft.dailydose.di.SnapshotsRootStorageQualifier
import com.hvasoft.dailydose.di.UsersDatabaseQualifier
import com.hvasoft.dailydose.domain.model.UserProfile
import com.hvasoft.dailydose.domain.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    @UsersDatabaseQualifier private val usersDatabase: DatabaseReference,
    @SnapshotsRootStorageQualifier private val snapshotsRootStorage: StorageReference,
) : ProfileRepository {

    override suspend fun loadUserProfile(userId: String): Result<UserProfile?> =
        withContext(Dispatchers.IO) {
            try {
                val snap = usersDatabase.child(userId).get().await()
                val u = snap.getValue(User::class.java)
                Result.success(
                    UserProfile(
                        userId = userId,
                        displayName = u?.userName.orEmpty(),
                        photoUrl = u?.photoUrl.orEmpty(),
                        email = "",
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun uploadProfilePhoto(
        userId: String,
        localImageContentUri: String,
        onProgress: (Int) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(localImageContentUri)
            val ref = snapshotsRootStorage.child(userId).child(PROFILE_IMAGE_FILE_NAME)
            val task = ref.putFile(uri)
            task.addOnProgressListener { taskSnapshot ->
                val total = taskSnapshot.totalByteCount
                onProgress(if (total > 0) (100 * taskSnapshot.bytesTransferred / total).toInt() else 0)
            }
            task.await()
            Result.success(task.snapshot.storage.downloadUrl.await().toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun mergeAndSaveUserRecord(
        userId: String,
        userName: String?,
        photoUrl: String?,
        fallbackDisplayName: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val snap = usersDatabase.child(userId).get().await()
            val current = snap.getValue(User::class.java) ?: User()
            val updated = current.copy(
                userName = userName ?: current.userName.ifEmpty { fallbackDisplayName },
                photoUrl = photoUrl ?: current.photoUrl,
            )
            usersDatabase.child(userId).setValue(updated).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAuthDisplayName(newName: String): Result<Unit> =
        withContext(Dispatchers.Main) {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                    ?: return@withContext Result.failure(IllegalStateException("No signed-in user"))
                user.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(newName).build()
                ).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    companion object {
        private const val PROFILE_IMAGE_FILE_NAME = "userImageProfile"
    }
}
