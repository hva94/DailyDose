package com.hvasoft.dailydose.presentation.screens.home

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.domain.common.extension_functions.isCurrentUserOwner
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.presentation.screens.common.DefaultImageAspectRatio
import com.hvasoft.dailydose.presentation.screens.common.ShimmerPlaceholder
import com.hvasoft.dailydose.presentation.screens.common.calculateClampedAspectRatio
import com.hvasoft.dailydose.presentation.screens.common.formatRelativeTime
import com.hvasoft.dailydose.presentation.theme.DailyDoseTheme
import com.hvasoft.dailydose.presentation.theme.Unselected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val FILE_PROVIDER_AUTHORITY = "com.hvasoft.fileprovider"
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    refreshSignal: Int,
    modifier: Modifier = Modifier,
    onShowMessage: (Int) -> Unit,
) {
    val pagingItems = viewModel.snapshots.collectAsLazyPagingItems()
    val optimisticLikes = remember { mutableStateMapOf<String, SnapshotLikeState>() }
    var pendingDeleteSnapshot by remember { mutableStateOf<Snapshot?>(null) }
    var expandedImage by remember { mutableStateOf<ExpandedImageState?>(null) }
    var shouldScrollToTop by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { messageRes ->
            onShowMessage(messageRes)
        }
    }

    LaunchedEffect(refreshSignal) {
        optimisticLikes.clear()
        shouldScrollToTop = refreshSignal > 0
        viewModel.fetchSnapshots()
    }

    LaunchedEffect(
        shouldScrollToTop,
        pagingItems.loadState.refresh,
        pagingItems.itemCount,
    ) {
        val refreshState = pagingItems.loadState.refresh
        if (!shouldScrollToTop) return@LaunchedEffect

        if (refreshState is LoadState.NotLoading) {
            if (pagingItems.itemCount > 0) {
                listState.animateScrollToItem(0)
            }
            shouldScrollToTop = false
        }
    }

    HomeScreen(
        pagingItems = pagingItems,
        optimisticLikes = optimisticLikes,
        listState = listState,
        modifier = modifier,
        onRetry = viewModel::fetchSnapshots,
        onLikeToggle = { snapshot, isLiked ->
            optimisticLikes[snapshot.snapshotKey] = SnapshotLikeState(
                isLiked = isLiked,
                likeCount = computeLikeCount(snapshot.likeCount, snapshot.isLikedByCurrentUser, isLiked),
            )
            viewModel.setLikeSnapshot(snapshot, isLiked)
        },
        onShare = { snapshot ->
            scope.launch {
                runCatching {
                    shareSnapshot(context, snapshot)
                }.onFailure {
                    onShowMessage(R.string.home_share_image_error)
                }
            }
        },
        onOpenImage = { snapshot ->
            snapshot.photoUrl?.takeIf { it.isNotBlank() }?.let { url ->
                expandedImage = ExpandedImageState(
                    imageUrl = url,
                    title = snapshot.title.orEmpty(),
                )
            }
        },
        onRequestDelete = { pendingDeleteSnapshot = it },
    )

    if (pendingDeleteSnapshot != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteSnapshot = null },
            title = { Text(text = stringResource(R.string.dialog_delete_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteSnapshot?.let { snapshot ->
                            optimisticLikes.remove(snapshot.snapshotKey)
                            viewModel.deleteSnapshot(snapshot)
                        }
                        pendingDeleteSnapshot = null
                    },
                ) {
                    Text(text = stringResource(R.string.dialog_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSnapshot = null }) {
                    Text(text = stringResource(R.string.dialog_delete_cancel))
                }
            },
        )
    }

    expandedImage?.let { imageState ->
        ExpandedImageViewer(
            imageUrl = imageState.imageUrl,
            title = imageState.title,
            onDismiss = { expandedImage = null },
        )
    }
}

@Composable
private fun HomeScreen(
    pagingItems: LazyPagingItems<Snapshot>,
    optimisticLikes: Map<String, SnapshotLikeState>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
    onLikeToggle: (Snapshot, Boolean) -> Unit,
    onShare: (Snapshot) -> Unit,
    onOpenImage: (Snapshot) -> Unit,
    onRequestDelete: (Snapshot) -> Unit,
) {
    val context = LocalContext.current
    val loadState = pagingItems.loadState.refresh
    val isInitialLoading = loadState is LoadState.Loading && pagingItems.itemCount == 0
    val isError = loadState is LoadState.Error && pagingItems.itemCount == 0
    val isEmpty = loadState is LoadState.NotLoading && pagingItems.itemCount == 0

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            isInitialLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            isError -> {
                ErrorState(
                    modifier = Modifier.align(Alignment.Center),
                    onRetry = onRetry,
                )
            }

            isEmpty -> {
                EmptyState(
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(
                        count = pagingItems.itemCount,
                        key = { index: Int -> pagingItems[index]?.snapshotKey ?: index },
                    ) { index: Int ->
                        val snapshot = pagingItems[index] ?: return@items
                        val likeState = optimisticLikes[snapshot.snapshotKey]
                        SnapshotCard(
                            snapshot = snapshot,
                            isLiked = likeState?.isLiked ?: snapshot.isLikedByCurrentUser,
                            likeCount = likeState?.likeCount ?: snapshot.likeCount.toIntOrNull() ?: 0,
                            onLikeToggle = { onLikeToggle(snapshot, it) },
                            onDelete = { onRequestDelete(snapshot) },
                            onShare = { onShare(snapshot) },
                            onOpenImage = { onOpenImage(snapshot) },
                            isPreview = LocalInspectionMode.current,
                            context = context,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SnapshotCard(
    snapshot: Snapshot,
    isLiked: Boolean,
    likeCount: Int,
    onLikeToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onOpenImage: () -> Unit,
    isPreview: Boolean,
    context: Context,
) {
    val resources = context.resources
    val profilePlaceholder = painterResource(R.drawable.image_placeholder)
    val imagePlaceholder = painterResource(R.drawable.image_placeholder)
    var imageAspectRatio by remember(snapshot.snapshotKey) {
        mutableFloatStateOf(DefaultImageAspectRatio)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SubcomposeAsyncImage(
                    model = snapshot.userPhotoUrl,
                    contentDescription = stringResource(R.string.home_description_profile_user_photo),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.extraLarge),
                    contentScale = ContentScale.Crop,
                    loading = {
                        ShimmerPlaceholder(
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                    error = {
                        Image(
                            painter = profilePlaceholder,
                            contentDescription = stringResource(R.string.home_description_profile_user_photo),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    },
                )
                Spacer(modifier = Modifier.size(8.dp))
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = snapshot.userName.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = formatRelativeTime(resources, snapshot.dateTime),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (snapshot.isCurrentUserOwner()) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.home_description_button_delete),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            SubcomposeAsyncImage(
                model = if (isPreview) R.drawable.image_placeholder else snapshot.photoUrl,
                contentDescription = stringResource(R.string.home_description_img_publication_user),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .aspectRatio(imageAspectRatio)
                    .clickable(
                        enabled = !isPreview && !snapshot.photoUrl.isNullOrBlank(),
                        onClick = onOpenImage,
                    ),
                contentScale = ContentScale.Crop,
                loading = {
                    ShimmerPlaceholder(
                        modifier = Modifier.fillMaxSize(),
                    )
                },
                error = {
                    Image(
                        painter = imagePlaceholder,
                        contentDescription = stringResource(R.string.home_description_img_publication_user),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                },
                onSuccess = { state ->
                    imageAspectRatio = calculateClampedAspectRatio(
                        width = state.result.drawable.intrinsicWidth,
                        height = state.result.drawable.intrinsicHeight,
                    )
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    IconToggleButton(
                        checked = isLiked,
                        onCheckedChange = onLikeToggle,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_drop_dailydose),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(
                                if (isLiked) MaterialTheme.colorScheme.primary else Unselected,
                            ),
                        )
                    }
                    Text(
                        text = likeCount.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = snapshot.title.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                IconButton(
                    onClick = onShare,
                    enabled = !isPreview,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = stringResource(R.string.home_description_button_share),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedImageViewer(
    imageUrl: String,
    title: String,
    onDismiss: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val imagePlaceholder = painterResource(R.drawable.image_placeholder)

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = newScale
        offset = if (newScale > 1f) {
            clampOffset(offset + panChange, newScale, containerSize)
        } else {
            Offset.Zero
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp)
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                val zoomScale = 2.5f
                                scale = zoomScale
                                val centeredOffset = Offset(
                                    x = (containerSize.width / 2f - tapOffset.x) * (zoomScale - 1f),
                                    y = (containerSize.height / 2f - tapOffset.y) * (zoomScale - 1f),
                                )
                                offset = clampOffset(centeredOffset, zoomScale, containerSize)
                            }
                        },
                    )
                },
        ) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = stringResource(R.string.home_description_img_publication_user),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .onSizeChanged { containerSize = it }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .transformable(state = transformableState)
                    .background(Color.Black),
                contentScale = ContentScale.Fit,
                loading = {
                    ShimmerPlaceholder(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                    )
                },
                error = {
                    Image(
                        painter = imagePlaceholder,
                        contentDescription = stringResource(R.string.home_description_img_publication_user),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentScale = ContentScale.Fit,
                    )
                },
            )

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_image_viewer_close),
                    color = Color.White,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = stringResource(R.string.home_image_viewer_hint),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.empty_state),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.text_no_snapshots),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.text_empty_state_snapshots),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ErrorState(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.home_database_access_error),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry_text))
        }
    }
}

private suspend fun shareSnapshot(
    context: Context,
    snapshot: Snapshot,
) {
    val shareText = context.getString(R.string.home_description_button_share, snapshot.title)
    val imageUrl = snapshot.photoUrl
    if (imageUrl.isNullOrBlank()) {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                },
                context.getString(R.string.home_description_title_share),
            )
        )
        return
    }

    val imageUri = withContext(Dispatchers.IO) {
        createShareableImageUri(context, imageUrl, snapshot.snapshotKey)
    }
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                clipData = ClipData.newUri(context.contentResolver, "snapshot_image", imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            context.getString(R.string.home_description_title_share),
        )
    )
}

private suspend fun createShareableImageUri(
    context: Context,
    imageUrl: String,
    snapshotKey: String,
): Uri {
    val request = ImageRequest.Builder(context)
        .data(imageUrl.toUri())
        .allowHardware(false)
        .build()
    val result = context.imageLoader.execute(request)
    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
        ?: error("Unable to decode image")
    val shareDirectory = File(context.cacheDir, "shared_images").apply {
        mkdirs()
    }
    val shareFile = File(shareDirectory, "$snapshotKey.jpg")
    FileOutputStream(shareFile).use { output ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
    }
    return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, shareFile)
}

private fun computeLikeCount(
    currentLikeCount: String,
    currentlyLiked: Boolean,
    newLikeState: Boolean,
): Int {
    val parsedCount = currentLikeCount.toIntOrNull() ?: 0
    return when {
        currentlyLiked == newLikeState -> parsedCount
        newLikeState -> parsedCount + 1
        else -> (parsedCount - 1).coerceAtLeast(0)
    }
}

private fun clampOffset(
    offset: Offset,
    scale: Float,
    containerSize: IntSize,
): Offset {
    if (containerSize == IntSize.Zero || scale <= 1f) return Offset.Zero

    val maxX = ((containerSize.width * (scale - 1f)) / 2f).coerceAtLeast(0f)
    val maxY = ((containerSize.height * (scale - 1f)) / 2f).coerceAtLeast(0f)

    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY),
    )
}

private data class SnapshotLikeState(
    val isLiked: Boolean,
    val likeCount: Int,
)

private data class ExpandedImageState(
    val imageUrl: String,
    val title: String,
)

@Preview(showBackground = true)
@Composable
private fun SnapshotCardPreview() {
    DailyDoseTheme {
        SnapshotCard(
            snapshot = Snapshot(
                title = "A peaceful morning",
                dateTime = System.currentTimeMillis() - 3_600_000,
                photoUrl = "",
                userName = "Henry",
                userPhotoUrl = "",
                snapshotKey = "preview",
                likeCount = "7",
            ),
            isLiked = true,
            likeCount = 7,
            onLikeToggle = {},
            onDelete = {},
            onShare = {},
            onOpenImage = {},
            isPreview = true,
            context = LocalContext.current,
        )
    }
}
