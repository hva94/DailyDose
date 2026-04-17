package com.hvasoft.dailydose.presentation.screens.add

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.presentation.screens.common.DefaultImageAspectRatio
import com.hvasoft.dailydose.presentation.screens.common.calculateClampedAspectRatio
import com.hvasoft.dailydose.presentation.theme.DailyDoseTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val AddFileProviderAuthority = "com.hvasoft.fileprovider"

@Composable
fun AddRoute(
    viewModel: AddViewModel,
    modifier: Modifier = Modifier,
    onShowMessage: (Int) -> Unit,
    onSnapshotPosted: (Snapshot) -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var showImageSourceDialog by rememberSaveable { mutableStateOf(false) }
    var titleErrorRes by remember { mutableStateOf<Int?>(null) }
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    val photoPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                selectedImageUri = uri.toString()
                title = context.getString(R.string.add_default_title, currentTimeLabel())
                titleErrorRes = null
            }
        }
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                selectedImageUri = pendingCameraUri
                title = context.getString(R.string.add_default_title, currentTimeLabel())
                titleErrorRes = null
            } else {
                onShowMessage(R.string.add_take_picture_error)
            }
        }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            AddPostUiState.Idle -> Unit

            is AddPostUiState.Uploading -> Unit

            is AddPostUiState.Success -> {
                selectedImageUri = null
                title = ""
                titleErrorRes = null
                onShowMessage(R.string.post_message_post_success)
                runCatching {
                    onSnapshotPosted(state.snapshot)
                }.onFailure {
                    onShowMessage(R.string.error_unknown)
                }
                viewModel.acknowledgeTerminalState()
            }

            AddPostUiState.FailedImage -> {
                onShowMessage(R.string.post_message_post_image_fail)
                viewModel.acknowledgeTerminalState()
            }

            AddPostUiState.FailedSave -> {
                onShowMessage(R.string.post_message_post_fail)
                viewModel.acknowledgeTerminalState()
            }
        }
    }

    if (showImageSourceDialog) {
        ImageSourceDialog(
            onDismiss = { showImageSourceDialog = false },
            onTakePhoto = {
                showImageSourceDialog = false
                val outputUri = createCameraImageUri(context)
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

    AddScreenContent(
        modifier = modifier,
        title = title,
        titleErrorRes = titleErrorRes,
        imageUri = selectedImageUri?.let(Uri::parse),
        uiState = uiState,
        onTitleChange = {
            title = it
            if (it.isNotBlank()) titleErrorRes = null
        },
        onSelectImage = { showImageSourceDialog = true },
        onPost = {
            if (title.trim().isEmpty()) {
                titleErrorRes = R.string.helper_required
            } else if (selectedImageUri != null) {
                viewModel.postSnapshot(
                    title = title.trim(),
                    imageUri = Uri.parse(selectedImageUri),
                )
            }
        },
    )
}

@Composable
fun AddScreenContent(
    title: String,
    titleErrorRes: Int?,
    imageUri: Uri?,
    uiState: AddPostUiState,
    modifier: Modifier = Modifier,
    onTitleChange: (String) -> Unit,
    onSelectImage: () -> Unit,
    onPost: () -> Unit,
) {
    val isUploading = uiState is AddPostUiState.Uploading
    val uploadProgress = (uiState as? AddPostUiState.Uploading)?.percent ?: 0
    var imageAspectRatio by remember(imageUri) {
        mutableFloatStateOf(DefaultImageAspectRatio)
    }
    val message = when (uiState) {
        is AddPostUiState.Uploading -> "$uploadProgress%"
        else -> {
            if (imageUri == null) stringResource(R.string.post_message_title)
            else stringResource(R.string.post_message_valid_title)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        if (isUploading) {
            LinearProgressIndicator(
                progress = { uploadProgress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (imageUri != null) {
            Spacer(modifier = Modifier.height(4.dp))
        }
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
            )
            Button(
                onClick = onPost,
                enabled = !isUploading,
            ) {
                Text(
                    text = stringResource(R.string.add_button_post),
                    color = Color.White,
                )
            }
        }
        imageUri?.let {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUploading,
                label = { Text(text = stringResource(R.string.add_hint_title)) },
                isError = titleErrorRes != null,
                supportingText = {
                    titleErrorRes?.let {
                        Text(text = stringResource(titleErrorRes))
                    }
                },
                singleLine = true,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(imageAspectRatio)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = !isUploading, onClick = onSelectImage),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUri == null) {
                Image(
                    painter = painterResource(R.drawable.ic_photo_camera),
                    contentDescription = stringResource(R.string.add_button_select),
                )
            } else {
                AsyncImage(
                    model = imageUri,
                    contentDescription = stringResource(R.string.add_button_select),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onSuccess = { state ->
                        imageAspectRatio = calculateClampedAspectRatio(
                            width = state.result.drawable.intrinsicWidth,
                            height = state.result.drawable.intrinsicHeight,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ImageSourceDialog(
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

private fun createCameraImageUri(context: Context): Uri {
    val storageDirectory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val imageFile = File.createTempFile("photo", ".jpg", storageDirectory)
    return FileProvider.getUriForFile(context, AddFileProviderAuthority, imageFile)
}

private fun currentTimeLabel(): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(System.currentTimeMillis()))

@Preview(showBackground = true)
@Composable
private fun AddScreenPreview() {
    DailyDoseTheme {
        AddScreenContent(
            title = "My Daily Dose at 9:12 AM",
            titleErrorRes = null,
            imageUri = Uri.parse("content://preview"),
            uiState = AddPostUiState.Idle,
            onTitleChange = {},
            onSelectImage = {},
            onPost = {},
        )
    }
}
