package com.hvasoft.dailydose.presentation.screens.profile

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.firebase.ui.auth.AuthUI
import com.hvasoft.dailydose.BuildConfig
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.domain.model.UserProfile
import com.hvasoft.dailydose.presentation.screens.common.ShimmerPlaceholder
import com.hvasoft.dailydose.presentation.theme.DailyDoseTheme
import java.io.File

private const val ProfileFileProviderAuthority = "com.hvasoft.fileprovider"

@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel,
    refreshSignal: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onShowMessage: (Int) -> Unit,
    onSignedOut: () -> Unit,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val profileUiState by viewModel.profileUiState.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    var nameFieldValue by rememberSaveable { mutableStateOf("") }
    var isEditingName by rememberSaveable { mutableStateOf(false) }
    var showImageSourceDialog by rememberSaveable { mutableStateOf(false) }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var nameErrorRes by remember { mutableStateOf<Int?>(null) }
    val profile = profileUiState.profile
    val displayName = profile?.displayName.orEmpty()
    val email = profile?.email.orEmpty()
    val photoUrl = profile?.photoUrl.orEmpty()
    val localPhotoPath = profile?.localPhotoPath
    val authPhotoUrl = profileUiState.authPhotoUrl
    val canEditProfile = profile?.isOfflineFallback?.not() ?: false

    LaunchedEffect(displayName, canEditProfile) {
        nameFieldValue = displayName
        isEditingName = canEditProfile && displayName.isBlank()
        if (!canEditProfile) {
            nameErrorRes = null
        }
    }

    val photoPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                viewModel.uploadCurrentProfilePhoto(
                    imageUri = uri,
                    currentUserName = nameFieldValue.trim().ifEmpty { displayName },
                )
            }
        }
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = pendingCameraUri?.let(Uri::parse)
            if (success && uri != null) {
                viewModel.uploadCurrentProfilePhoto(
                    imageUri = uri,
                    currentUserName = nameFieldValue.trim().ifEmpty { displayName },
                )
            } else if (!success) {
                onShowMessage(R.string.post_message_post_image_fail)
            }
        }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileViewModel.Event.ProfileLoaded -> Unit

                is ProfileViewModel.Event.NameUpdated -> {
                    isEditingName = false
                    nameErrorRes = null
                    keyboardController?.hide()
                    onShowMessage(R.string.profile_name_updated)
                }

                is ProfileViewModel.Event.PhotoUpdated -> {
                    onShowMessage(R.string.profile_user_image_updated)
                }

                is ProfileViewModel.Event.Failure -> {
                    onShowMessage(event.messageRes)
                }
            }
        }
    }

    LaunchedEffect(refreshSignal) {
        viewModel.loadCurrentProfile()
    }

    if (showImageSourceDialog) {
        ProfileImageSourceDialog(
            onDismiss = { showImageSourceDialog = false },
            onTakePhoto = {
                showImageSourceDialog = false
                val outputUri = createProfileCameraImageUri(context)
                pendingCameraUri = outputUri.toString()
                cameraLauncher.launch(outputUri)
            },
            onChooseFromGallery = {
                showImageSourceDialog = false
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = stringResource(R.string.dialog_logout_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        AuthUI.getInstance().signOut(context).addOnCompleteListener {
                            Toast.makeText(
                                context,
                                R.string.profile_logout_success,
                                Toast.LENGTH_SHORT,
                            ).show()
                            nameFieldValue = ""
                            isEditingName = false
                            onSignedOut()
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.dialog_logout_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = stringResource(R.string.dialog_logout_cancel))
                }
            },
        )
    }

    ProfileScreen(
        contentPadding = contentPadding,
        modifier = modifier,
        displayName = displayName,
        nameFieldValue = nameFieldValue,
        email = email,
        photoUrl = photoUrl,
        localPhotoPath = localPhotoPath,
        authPhotoUrl = authPhotoUrl,
        canEditProfile = canEditProfile,
        isEditingName = isEditingName,
        uploadProgress = uploadProgress,
        nameErrorRes = nameErrorRes,
        versionLabel = context.getString(
            R.string.profile_version_label,
            BuildConfig.VERSION_NAME,
        ),
        onSelectImage = { showImageSourceDialog = true },
        onEditName = { isEditingName = true },
        onCancelName = {
            nameFieldValue = displayName
            isEditingName = canEditProfile && displayName.isBlank()
            nameErrorRes = null
            keyboardController?.hide()
        },
        onNameChange = {
            nameFieldValue = it
            if (it.isNotBlank()) nameErrorRes = null
        },
        onSaveName = {
            val newName = nameFieldValue.trim()
            if (newName.isEmpty()) {
                nameErrorRes = R.string.profile_name_empty_error
            } else {
                viewModel.updateCurrentDisplayName(newName)
            }
        },
        onLogout = { showLogoutDialog = true },
    )
}

@Composable
private fun ProfileScreen(
    displayName: String,
    nameFieldValue: String,
    email: String,
    photoUrl: String,
    localPhotoPath: String?,
    authPhotoUrl: String,
    canEditProfile: Boolean,
    isEditingName: Boolean,
    uploadProgress: Int?,
    nameErrorRes: Int?,
    versionLabel: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onSelectImage: () -> Unit,
    onEditName: () -> Unit,
    onCancelName: () -> Unit,
    onNameChange: (String) -> Unit,
    onSaveName: () -> Unit,
    onLogout: () -> Unit,
) {
    val photoModel = UserProfile(
        userId = "",
        displayName = displayName,
        photoUrl = photoUrl,
        localPhotoPath = localPhotoPath,
        email = email,
    ).preferredPhotoModel(authPhotoUrl)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .consumeWindowInsets(contentPadding)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.CenterVertically,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        uploadProgress?.let {
            CircularProgressIndicator(progress = { uploadProgress / 100f })
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .size(360.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(
                    enabled = canEditProfile,
                    onClick = onSelectImage,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (photoModel == null) {
                ProfileAvatarPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                SubcomposeAsyncImage(
                    model = photoModel,
                    contentDescription = stringResource(R.string.add_button_select),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    loading = {
                        ShimmerPlaceholder(
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                    error = {
                        ProfileAvatarPlaceholder(
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                )
            }
        }
        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineMedium,
        )
        if (isEditingName && canEditProfile) {
            OutlinedTextField(
                value = nameFieldValue,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.profile_hint_name)) },
                isError = nameErrorRes != null,
                supportingText = {
                    nameErrorRes?.let {
                        Text(text = stringResource(nameErrorRes))
                    }
                },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCancelName) {
                    Text(
                        text = stringResource(R.string.dialog_logout_cancel),
                        color = Color.White,
                    )
                }
                Button(onClick = onSaveName) {
                    Text(
                        text = stringResource(R.string.profile_button_save_name),
                        color = Color.White,
                    )
                }
            }
        } else if (canEditProfile) {
            Button(onClick = onEditName) {
                Text(
                    text = stringResource(R.string.profile_button_edit_name),
                    color = Color.White,
                )
            }
        }
        Text(
            text = email,
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onLogout) {
            Text(
                text = stringResource(R.string.button_logout),
                color = Color.White,
            )
        }
        Text(
            text = versionLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileAvatarPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_person),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(96.dp),
        )
    }
}

@Composable
private fun ProfileImageSourceDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit,
) {
    val options = stringArrayResource(R.array.array_options_item)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.dialog_options_title)) },
        confirmButton = {
            TextButton(onClick = onTakePhoto) {
                Text(text = options[0])
            }
        },
        dismissButton = {
            TextButton(onClick = onChooseFromGallery) {
                Text(text = options[1])
            }
        },
    )
}

private fun createProfileCameraImageUri(context: Context): Uri {
    val storageDirectory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val imageFile = File.createTempFile("photo", ".jpg", storageDirectory)
    return FileProvider.getUriForFile(context, ProfileFileProviderAuthority, imageFile)
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    DailyDoseTheme {
        ProfileScreen(
            displayName = "Henry Vazquez",
            nameFieldValue = "Henry Vazquez",
            email = "henry@example.com",
            photoUrl = "",
            localPhotoPath = null,
            authPhotoUrl = "",
            canEditProfile = true,
            isEditingName = false,
            uploadProgress = null,
            nameErrorRes = null,
            versionLabel = "Version 1.0.0",
            onSelectImage = {},
            onEditName = {},
            onCancelName = {},
            onNameChange = {},
            onSaveName = {},
            onLogout = {},
        )
    }
}
