package com.hvasoft.dailydose.presentation.screens.profile

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.auth.AuthSessionProvider
import com.hvasoft.dailydose.domain.interactor.profile.GetUserProfileUseCase
import com.hvasoft.dailydose.domain.interactor.profile.UpdateProfileNameUseCase
import com.hvasoft.dailydose.domain.interactor.profile.UploadProfilePhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authSessionProvider: AuthSessionProvider,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateProfileNameUseCase: UpdateProfileNameUseCase,
    private val uploadProfilePhotoUseCase: UploadProfilePhotoUseCase,
) : ViewModel() {

    sealed interface Event {
        data class ProfileLoaded(
            val displayName: String,
            val photoUrl: String,
            val email: String,
        ) : Event
        data class NameUpdated(val displayName: String) : Event
        data class PhotoUpdated(val photoUrl: String) : Event
        data class Failure(@StringRes val messageRes: Int) : Event
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private val _uploadProgress = MutableStateFlow<Int?>(null)
    val uploadProgress = _uploadProgress.asStateFlow()

    fun loadCurrentProfile() {
        viewModelScope.launch {
            val currentUser = authSessionProvider.currentUserSnapshotOrNull()
                ?: return@launch _events.emit(Event.Failure(R.string.error_unknown))

            getUserProfileUseCase(currentUser.userId).fold(
                onSuccess = { profile ->
                    val name = profile?.displayName?.ifBlank { currentUser.displayName }
                        ?: currentUser.displayName
                    _events.emit(
                        Event.ProfileLoaded(
                            displayName = name,
                            photoUrl = profile?.photoUrl.orEmpty(),
                            email = currentUser.email,
                        ),
                    )
                },
                onFailure = {
                    _events.emit(Event.Failure(R.string.home_database_access_error))
                },
            )
        }
    }

    fun updateCurrentDisplayName(newName: String) {
        viewModelScope.launch {
            val currentUser = authSessionProvider.currentUserSnapshotOrNull()
                ?: return@launch _events.emit(Event.Failure(R.string.error_unknown))

            updateProfileNameUseCase(
                currentUser.userId,
                newName,
                currentUser.displayName,
            ).fold(
                onSuccess = { _events.emit(Event.NameUpdated(newName)) },
                onFailure = { _events.emit(Event.Failure(R.string.profile_database_write_error)) },
            )
        }
    }

    fun uploadCurrentProfilePhoto(imageUri: Uri, currentUserName: String) {
        viewModelScope.launch {
            val currentUser = authSessionProvider.currentUserSnapshotOrNull()
                ?: return@launch _events.emit(Event.Failure(R.string.error_unknown))

            _uploadProgress.value = 0
            uploadProfilePhotoUseCase(
                userId = currentUser.userId,
                localImageContentUri = imageUri.toString(),
                currentUserName = currentUserName,
                onProgress = { percent -> _uploadProgress.value = percent },
            ).fold(
                onSuccess = { url ->
                    _uploadProgress.value = null
                    _events.emit(Event.PhotoUpdated(url))
                },
                onFailure = {
                    _uploadProgress.value = null
                    _events.emit(Event.Failure(R.string.post_message_post_image_fail))
                },
            )
        }
    }
}
