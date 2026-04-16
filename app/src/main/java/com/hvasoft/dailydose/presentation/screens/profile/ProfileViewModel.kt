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
import com.hvasoft.dailydose.domain.model.UserProfile
import com.hvasoft.dailydose.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authSessionProvider: AuthSessionProvider,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateProfileNameUseCase: UpdateProfileNameUseCase,
    private val uploadProfilePhotoUseCase: UploadProfilePhotoUseCase,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    data class ProfileUiState(
        val profile: UserProfile? = null,
        val authPhotoUrl: String = "",
    )

    sealed interface Event {
        data class ProfileLoaded(
            val profile: UserProfile,
            val authPhotoUrl: String,
            val email: String,
        ) : Event
        data class NameUpdated(val displayName: String) : Event
        data class PhotoUpdated(
            val profile: UserProfile,
            val authPhotoUrl: String,
        ) : Event
        data class Failure(@StringRes val messageRes: Int) : Event
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private val _profileUiState = MutableStateFlow(ProfileUiState())
    val profileUiState = _profileUiState.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Int?>(null)
    val uploadProgress = _uploadProgress.asStateFlow()

    fun seedSignedInUser(
        userId: String,
        displayName: String,
        email: String,
        photoUrl: String,
    ) {
        val resolvedDisplayName = displayName
            .takeIf(String::isNotBlank)
            ?: email.substringBefore('@', "")
        _profileUiState.update { currentState ->
            val existingProfile = currentState.profile
            ProfileUiState(
                profile = UserProfile(
                    userId = userId,
                    displayName = existingProfile?.displayName
                        ?.takeIf(String::isNotBlank)
                        ?: resolvedDisplayName,
                    photoUrl = existingProfile?.photoUrl
                        ?.takeIf(String::isNotBlank)
                        ?: photoUrl,
                    localPhotoPath = existingProfile?.localPhotoPath,
                    email = existingProfile?.email
                        ?.takeIf(String::isNotBlank)
                        ?: email,
                    isOfflineFallback = existingProfile?.isOfflineFallback ?: true,
                ),
                authPhotoUrl = photoUrl,
            )
        }
        viewModelScope.launch {
            refreshCachedAvatarLocalPath(userId)
        }
    }

    fun clearProfileState() {
        _profileUiState.value = ProfileUiState()
        _uploadProgress.value = null
    }

    fun loadCurrentProfile() {
        viewModelScope.launch {
            val currentUser = authSessionProvider.currentUserSnapshotOrNull()
                ?: return@launch _events.emit(Event.Failure(R.string.error_unknown))

            getUserProfileUseCase(currentUser.userId).fold(
                onSuccess = { profile ->
                    val existingProfile = _profileUiState.value.profile
                    val resolvedLocalPhotoPath = resolveLocalPhotoPath(
                        currentLocalPhotoPath = profile?.localPhotoPath
                            ?: existingProfile?.localPhotoPath,
                        userId = currentUser.userId,
                    )
                    val name = resolveDisplayName(
                        profileName = profile?.displayName,
                        currentUser = currentUser,
                    )
                    val resolvedEmail = resolveEmail(
                        profileEmail = profile?.email,
                        currentUser = currentUser,
                    )
                    val resolvedProfile = profile?.copy(
                        displayName = name,
                        photoUrl = profile.photoUrl
                            .takeIf(String::isNotBlank)
                            ?: existingProfile?.photoUrl.orEmpty(),
                        email = resolvedEmail,
                        localPhotoPath = resolvedLocalPhotoPath,
                    )
                        ?: UserProfile(
                            userId = currentUser.userId,
                            displayName = name,
                            photoUrl = currentUser.photoUrl
                                .takeIf(String::isNotBlank)
                                ?: existingProfile?.photoUrl.orEmpty(),
                            localPhotoPath = resolvedLocalPhotoPath,
                            email = resolvedEmail,
                            isOfflineFallback = true,
                        )
                    _profileUiState.value = ProfileUiState(
                        profile = resolvedProfile,
                        authPhotoUrl = currentUser.photoUrl
                            .takeIf(String::isNotBlank)
                            ?: _profileUiState.value.authPhotoUrl,
                    )
                    _events.emit(
                        Event.ProfileLoaded(
                            profile = resolvedProfile,
                            authPhotoUrl = currentUser.photoUrl,
                            email = resolvedEmail,
                        ),
                    )
                },
                onFailure = {
                    val existingProfile = _profileUiState.value.profile
                    val resolvedLocalPhotoPath = resolveLocalPhotoPath(
                        currentLocalPhotoPath = existingProfile?.localPhotoPath,
                        userId = currentUser.userId,
                    )
                    val resolvedEmail = resolveEmail(
                        profileEmail = null,
                        currentUser = currentUser,
                    )
                    _events.emit(
                        Event.ProfileLoaded(
                            profile = UserProfile(
                                userId = currentUser.userId,
                                displayName = resolveDisplayName(
                                    profileName = null,
                                    currentUser = currentUser,
                                ),
                                photoUrl = currentUser.photoUrl
                                    .takeIf(String::isNotBlank)
                                    ?: existingProfile?.photoUrl.orEmpty(),
                                localPhotoPath = resolvedLocalPhotoPath,
                                email = resolvedEmail,
                                isOfflineFallback = true,
                            ),
                            authPhotoUrl = currentUser.photoUrl
                                .takeIf(String::isNotBlank)
                                ?: _profileUiState.value.authPhotoUrl,
                            email = resolvedEmail,
                        ),
                    )
                    _profileUiState.value = ProfileUiState(
                        profile = UserProfile(
                            userId = currentUser.userId,
                            displayName = resolveDisplayName(
                                profileName = null,
                                currentUser = currentUser,
                            ),
                            photoUrl = currentUser.photoUrl
                                .takeIf(String::isNotBlank)
                                ?: existingProfile?.photoUrl.orEmpty(),
                            localPhotoPath = resolvedLocalPhotoPath,
                            email = resolvedEmail,
                            isOfflineFallback = true,
                        ),
                        authPhotoUrl = currentUser.photoUrl
                            .takeIf(String::isNotBlank)
                            ?: _profileUiState.value.authPhotoUrl,
                    )
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
                onSuccess = {
                    _profileUiState.update { currentState ->
                        currentState.copy(
                            profile = currentState.profile?.copy(
                                displayName = newName,
                                isOfflineFallback = false,
                            ),
                        )
                    }
                    _events.emit(Event.NameUpdated(newName))
                },
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
                onSuccess = { profile ->
                    _uploadProgress.value = null
                    _profileUiState.value = ProfileUiState(
                        profile = profile,
                        authPhotoUrl = currentUser.photoUrl,
                    )
                    _events.emit(
                        Event.PhotoUpdated(
                            profile = profile,
                            authPhotoUrl = currentUser.photoUrl,
                        ),
                    )
                },
                onFailure = {
                    _uploadProgress.value = null
                    _events.emit(Event.Failure(R.string.post_message_post_image_fail))
                },
            )
        }
    }

    private fun resolveDisplayName(
        profileName: String?,
        currentUser: com.hvasoft.dailydose.data.auth.CurrentUserSnapshot,
    ): String = profileName
        ?.takeIf(String::isNotBlank)
        ?: currentUser.displayName.takeIf(String::isNotBlank)
        ?: currentUser.email.substringBefore('@', "")

    private fun resolveEmail(
        profileEmail: String?,
        currentUser: com.hvasoft.dailydose.data.auth.CurrentUserSnapshot,
    ): String = profileEmail
        ?.takeIf(String::isNotBlank)
        ?: currentUser.email

    private suspend fun resolveLocalPhotoPath(
        currentLocalPhotoPath: String?,
        userId: String,
    ): String? = currentLocalPhotoPath
        ?.takeIf(String::isNotBlank)
        ?: profileRepository.getCachedAvatarLocalPath(userId)

    private suspend fun refreshCachedAvatarLocalPath(userId: String) {
        val cachedAvatarLocalPath = profileRepository.getCachedAvatarLocalPath(userId) ?: return
        _profileUiState.update { currentState ->
            currentState.copy(
                profile = currentState.profile?.copy(
                    localPhotoPath = cachedAvatarLocalPath,
                ),
            )
        }
    }
}
