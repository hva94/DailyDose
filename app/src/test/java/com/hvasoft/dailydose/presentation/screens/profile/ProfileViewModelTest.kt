package com.hvasoft.dailydose.presentation.screens.profile

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.hvasoft.dailydose.MainDispatcherRule
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.auth.FakeAuthSessionProvider
import com.hvasoft.dailydose.domain.interactor.profile.GetUserProfileUseCase
import com.hvasoft.dailydose.domain.interactor.profile.UpdateProfileNameUseCase
import com.hvasoft.dailydose.domain.interactor.profile.UploadProfilePhotoUseCase
import com.hvasoft.dailydose.domain.model.UserProfile
import com.hvasoft.dailydose.domain.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authSessionProvider: FakeAuthSessionProvider
    private lateinit var getUserProfileUseCase: GetUserProfileUseCase
    private lateinit var updateProfileNameUseCase: UpdateProfileNameUseCase
    private lateinit var uploadProfilePhotoUseCase: UploadProfilePhotoUseCase
    private lateinit var profileRepository: ProfileRepository
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        authSessionProvider = FakeAuthSessionProvider()
        getUserProfileUseCase = mockk()
        updateProfileNameUseCase = mockk()
        uploadProfilePhotoUseCase = mockk()
        profileRepository = mockk()
        coEvery { profileRepository.getCachedAvatarLocalPath(any()) } returns null
        viewModel = ProfileViewModel(
            authSessionProvider = authSessionProvider,
            getUserProfileUseCase = getUserProfileUseCase,
            updateProfileNameUseCase = updateProfileNameUseCase,
            uploadProfilePhotoUseCase = uploadProfilePhotoUseCase,
            profileRepository = profileRepository,
        )
    }

    @Test
    fun `loadCurrentProfile emits failure when signed out`() = runTest {
        val event = async { viewModel.events.first() }

        viewModel.loadCurrentProfile()
        advanceUntilIdle()

        assertThat(event.await()).isEqualTo(ProfileViewModel.Event.Failure(R.string.error_unknown))
        coVerify(exactly = 0) { getUserProfileUseCase.invoke(any()) }
    }

    @Test
    fun `loadCurrentProfile merges auth fallback fields when signed in`() = runTest {
        signInUser(displayName = "Henry", email = "henry@example.com")
        coEvery { getUserProfileUseCase.invoke("user-123") } returns Result.success(
            UserProfile(
                userId = "user-123",
                displayName = "",
                photoUrl = "https://example.com/photo.jpg",
                localPhotoPath = "/tmp/profile.jpg",
                email = "",
                isOfflineFallback = false,
            ),
        )
        val event = async { viewModel.events.first() }

        viewModel.loadCurrentProfile()
        advanceUntilIdle()

        assertThat(event.await()).isEqualTo(
            ProfileViewModel.Event.ProfileLoaded(
                profile = UserProfile(
                    userId = "user-123",
                    displayName = "Henry",
                    photoUrl = "https://example.com/photo.jpg",
                    localPhotoPath = "/tmp/profile.jpg",
                    email = "henry@example.com",
                    isOfflineFallback = false,
                ),
                authPhotoUrl = "",
                email = "henry@example.com",
            ),
        )
    }

    @Test
    fun `loadCurrentProfile emits offline auth-backed profile when repository load fails`() = runTest {
        signInUser(
            displayName = "",
            email = "henry@example.com",
            photoUrl = "https://example.com/auth-photo.jpg",
        )
        coEvery { getUserProfileUseCase.invoke("user-123") } returns Result.failure(
            IllegalStateException("offline"),
        )
        val event = async { viewModel.events.first() }

        viewModel.loadCurrentProfile()
        advanceUntilIdle()

        assertThat(event.await()).isEqualTo(
            ProfileViewModel.Event.ProfileLoaded(
                profile = UserProfile(
                    userId = "user-123",
                    displayName = "henry",
                    photoUrl = "https://example.com/auth-photo.jpg",
                    localPhotoPath = null,
                    email = "henry@example.com",
                    isOfflineFallback = true,
                ),
                authPhotoUrl = "https://example.com/auth-photo.jpg",
                email = "henry@example.com",
            ),
        )
    }

    @Test
    fun `loadCurrentProfile still emits auth-backed profile when repository falls back offline`() = runTest {
        signInUser(displayName = "Henry", email = "henry@example.com")
        coEvery { getUserProfileUseCase.invoke("user-123") } returns Result.success(
            UserProfile(
                userId = "user-123",
                displayName = "",
                photoUrl = "https://example.com/cached-photo.jpg",
                localPhotoPath = "/tmp/profile-cached.jpg",
                email = "cached@example.com",
                isOfflineFallback = true,
            ),
        )
        val event = async { viewModel.events.first() }

        viewModel.loadCurrentProfile()
        advanceUntilIdle()

        assertThat(event.await()).isEqualTo(
            ProfileViewModel.Event.ProfileLoaded(
                profile = UserProfile(
                    userId = "user-123",
                    displayName = "Henry",
                    photoUrl = "https://example.com/cached-photo.jpg",
                    localPhotoPath = "/tmp/profile-cached.jpg",
                    email = "cached@example.com",
                    isOfflineFallback = true,
                ),
                authPhotoUrl = "",
                email = "cached@example.com",
            ),
        )
    }

    @Test
    fun `loadCurrentProfile fills missing local photo path from cached avatar path`() = runTest {
        signInUser(displayName = "Henry", email = "henry@example.com")
        coEvery { getUserProfileUseCase.invoke("user-123") } returns Result.success(
            UserProfile(
                userId = "user-123",
                displayName = "Henry",
                photoUrl = "https://example.com/cached-photo.jpg",
                localPhotoPath = null,
                email = "henry@example.com",
                isOfflineFallback = true,
            ),
        )
        coEvery { profileRepository.getCachedAvatarLocalPath("user-123") } returns "/tmp/profile-cached.jpg"
        val event = async { viewModel.events.first() }

        viewModel.loadCurrentProfile()
        advanceUntilIdle()

        assertThat(event.await()).isEqualTo(
            ProfileViewModel.Event.ProfileLoaded(
                profile = UserProfile(
                    userId = "user-123",
                    displayName = "Henry",
                    photoUrl = "https://example.com/cached-photo.jpg",
                    localPhotoPath = "/tmp/profile-cached.jpg",
                    email = "henry@example.com",
                    isOfflineFallback = true,
                ),
                authPhotoUrl = "",
                email = "henry@example.com",
            ),
        )
    }

    @Test
    fun `loadCurrentProfile preserves existing avatar when offline reload returns blank image fields`() = runTest {
        signInUser(displayName = "Henry", email = "henry@example.com")
        coEvery { getUserProfileUseCase.invoke("user-123") } returns Result.success(
            UserProfile(
                userId = "user-123",
                displayName = "Henry",
                photoUrl = "",
                localPhotoPath = "/tmp/profile-existing.jpg",
                email = "henry@example.com",
                isOfflineFallback = true,
            ),
        )

        viewModel.loadCurrentProfile()
        advanceUntilIdle()

        coEvery { getUserProfileUseCase.invoke("user-123") } returns Result.success(
            UserProfile(
                userId = "user-123",
                displayName = "Henry",
                photoUrl = "",
                localPhotoPath = null,
                email = "henry@example.com",
                isOfflineFallback = true,
            ),
        )
        val event = async { viewModel.events.first() }

        viewModel.loadCurrentProfile()
        advanceUntilIdle()

        assertThat(event.await()).isEqualTo(
            ProfileViewModel.Event.ProfileLoaded(
                profile = UserProfile(
                    userId = "user-123",
                    displayName = "Henry",
                    photoUrl = "",
                    localPhotoPath = "/tmp/profile-existing.jpg",
                    email = "henry@example.com",
                    isOfflineFallback = true,
                ),
                authPhotoUrl = "",
                email = "henry@example.com",
            ),
        )
    }

    @Test
    fun `seedSignedInUser applies cached avatar local path to ui state`() = runTest {
        coEvery { profileRepository.getCachedAvatarLocalPath("user-123") } returns "/tmp/profile-seeded.jpg"

        viewModel.seedSignedInUser(
            userId = "user-123",
            displayName = "Henry",
            email = "henry@example.com",
            photoUrl = "",
        )
        advanceUntilIdle()

        assertThat(viewModel.profileUiState.value.profile).isEqualTo(
            UserProfile(
                userId = "user-123",
                displayName = "Henry",
                photoUrl = "",
                localPhotoPath = "/tmp/profile-seeded.jpg",
                email = "henry@example.com",
                isOfflineFallback = true,
            ),
        )
    }

    @Test
    fun `updateCurrentDisplayName emits failure when signed out`() = runTest {
        val event = async { viewModel.events.first() }

        viewModel.updateCurrentDisplayName("New Name")
        advanceUntilIdle()

        assertThat(event.await()).isEqualTo(ProfileViewModel.Event.Failure(R.string.error_unknown))
        coVerify(exactly = 0) { updateProfileNameUseCase.invoke(any(), any(), any()) }
    }

    @Test
    fun `updateCurrentDisplayName emits updated name when signed in`() = runTest {
        signInUser(displayName = "Henry")
        coEvery { updateProfileNameUseCase.invoke("user-123", "New Name", "Henry") } returns Result.success(Unit)
        val event = async { viewModel.events.first() }

        viewModel.updateCurrentDisplayName("New Name")
        advanceUntilIdle()

        assertThat(event.await()).isEqualTo(ProfileViewModel.Event.NameUpdated("New Name"))
    }

    @Test
    fun `uploadCurrentProfilePhoto emits failure when signed out`() = runTest {
        val event = async { viewModel.events.first() }
        val imageUri = mockUri()

        viewModel.uploadCurrentProfilePhoto(
            imageUri = imageUri,
            currentUserName = "Henry",
        )
        advanceUntilIdle()

        assertThat(event.await()).isEqualTo(ProfileViewModel.Event.Failure(R.string.error_unknown))
        coVerify(exactly = 0) { uploadProfilePhotoUseCase.invoke(any(), any(), any(), any()) }
    }

    @Test
    fun `uploadCurrentProfilePhoto emits updated photo when signed in`() = runTest {
        signInUser(displayName = "Henry")
        val imageUri = mockUri()
        coEvery {
            uploadProfilePhotoUseCase.invoke(
                userId = "user-123",
                localImageContentUri = "content://images/test",
                currentUserName = "Henry",
                onProgress = any(),
            )
        } coAnswers {
            val onProgress = arg<(Int) -> Unit>(3)
            onProgress(50)
            Result.success(
                UserProfile(
                    userId = "user-123",
                    displayName = "Henry",
                    photoUrl = "https://example.com/updated-photo.jpg",
                    localPhotoPath = "/tmp/profile-updated.jpg",
                    email = "",
                    isOfflineFallback = false,
                ),
            )
        }
        val event = async { viewModel.events.first() }

        viewModel.uploadCurrentProfilePhoto(
            imageUri = imageUri,
            currentUserName = "Henry",
        )
        advanceUntilIdle()

        assertThat(viewModel.uploadProgress.value).isNull()
        assertThat(event.await()).isEqualTo(
            ProfileViewModel.Event.PhotoUpdated(
                profile = UserProfile(
                    userId = "user-123",
                    displayName = "Henry",
                    photoUrl = "https://example.com/updated-photo.jpg",
                    localPhotoPath = "/tmp/profile-updated.jpg",
                    email = "",
                    isOfflineFallback = false,
                ),
                authPhotoUrl = "",
            ),
        )
    }

    private fun signInUser(
        displayName: String,
        email: String = "",
        photoUrl: String? = null,
    ) {
        val currentUser = mockk<FirebaseUser>()
        val photoUri = photoUrl?.let { url ->
            mockk<Uri>().also { uri ->
                every { uri.toString() } returns url
            }
        }
        every { currentUser.uid } returns "user-123"
        every { currentUser.displayName } returns displayName
        every { currentUser.email } returns email
        every { currentUser.photoUrl } returns photoUri
        authSessionProvider.setCurrentUser(currentUser)
    }

    private fun mockUri(): Uri = mockk<Uri>().also { uri ->
        every { uri.toString() } returns "content://images/test"
    }
}
