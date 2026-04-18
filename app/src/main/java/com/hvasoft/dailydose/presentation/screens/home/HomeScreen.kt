package com.hvasoft.dailydose.presentation.screens.home

import android.content.Context
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.SubcomposeAsyncImage
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.domain.common.extension_functions.isOwnedBy
import com.hvasoft.dailydose.domain.model.HomeFeedAvailabilityMode
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.model.SnapshotReply
import com.hvasoft.dailydose.domain.model.SnapshotReplyDeliveryState
import com.hvasoft.dailydose.presentation.screens.common.DefaultImageAspectRatio
import com.hvasoft.dailydose.presentation.screens.common.ShimmerPlaceholder
import com.hvasoft.dailydose.presentation.screens.common.calculateClampedAspectRatio
import com.hvasoft.dailydose.presentation.screens.common.formatRelativeTime
import com.hvasoft.dailydose.presentation.screens.common.formatReplyCount
import com.hvasoft.dailydose.presentation.theme.DailyDoseTheme
import com.hvasoft.dailydose.presentation.theme.PrimaryLight
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private const val white_hearth = "\uD83E\uDD0D"
private const val purple_hearth = "\uD83D\uDC9C"
private val ReactionOptions = listOf(purple_hearth, "\ud83d\udd25", "\ud83d\ude02", "\ud83d\ude2e", "\ud83d\ude22", "\ud83d\ude21")

private const val ReactionButtonTag = "snapshot_reaction_button"
private const val ReplyButtonTag = "snapshot_reply_button"
private const val ReplyInputTag = "snapshot_reply_input"
private const val ReplySendButtonTag = "snapshot_reply_send_button"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    scrollSignal: Int,
    modifier: Modifier = Modifier,
    onShowMessage: (Int) -> Unit,
) {
    val pagingItems = viewModel.snapshots.collectAsLazyPagingItems()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val replySheetState by viewModel.replySheetState.collectAsStateWithLifecycle()
    var pendingDeleteSnapshot by remember { mutableStateOf<Snapshot?>(null) }
    var expandedImage by remember { mutableStateOf<ExpandedImageState?>(null) }
    var shouldScrollToTop by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val currentUserId = viewModel.currentUserIdOrNull()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { messageRes ->
            onShowMessage(messageRes)
        }
    }

    LaunchedEffect(scrollSignal) {
        shouldScrollToTop = scrollSignal > 0
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
        uiState = uiState,
        listState = listState,
        currentUserId = currentUserId,
        modifier = modifier,
        onRetry = viewModel::retrySync,
        onRefresh = viewModel::retrySync,
        onReactionSelected = viewModel::setSnapshotReaction,
        onOpenReplies = viewModel::openReplies,
        onShare = { snapshot ->
            val canUseRemote = uiState.actionPolicy == HomeFeedActionPolicy.FULL_ACCESS
            if (snapshot.canShareImage(canUseRemote).not()) {
                onShowMessage(R.string.home_share_image_unavailable_offline)
                return@HomeScreen
            }

            scope.launch {
                runCatching {
                    shareSnapshot(context, snapshot, canUseRemote)
                }.onFailure {
                    onShowMessage(R.string.home_share_image_error)
                }
            }
        },
        onOpenImage = { snapshot ->
            val imageModel = snapshot.preferredPhotoModel(
                allowRemoteFallback = uiState.actionPolicy == HomeFeedActionPolicy.FULL_ACCESS,
            )
            if (imageModel == null) {
                onShowMessage(R.string.home_image_unavailable_offline)
                return@HomeScreen
            }

            expandedImage = ExpandedImageState(
                imageModel = imageModel,
                title = snapshot.title.orEmpty(),
            )
        },
        onRequestDelete = { snapshot ->
            if (uiState.actionPolicy == HomeFeedActionPolicy.READ_ONLY_OFFLINE) {
                onShowMessage(R.string.home_delete_offline_unavailable)
            } else {
                pendingDeleteSnapshot = snapshot
            }
        },
    )

    if (replySheetState.isVisible) {
        SnapshotReplySheet(
            state = replySheetState,
            onDismiss = viewModel::closeReplies,
            onDraftChange = viewModel::updateReplyComposer,
            onRetry = viewModel::retryReplies,
            onSubmit = viewModel::submitReply,
        )
    }

    pendingDeleteSnapshot?.let {
        AlertDialog(
            onDismissRequest = { pendingDeleteSnapshot = null },
            title = { Text(text = stringResource(R.string.dialog_delete_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteSnapshot?.let { snapshot ->
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
            imageModel = imageState.imageModel,
            title = imageState.title,
            onDismiss = { expandedImage = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    pagingItems: LazyPagingItems<Snapshot>,
    uiState: HomeFeedUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    currentUserId: String?,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onReactionSelected: (Snapshot, String) -> Unit,
    onOpenReplies: (Snapshot) -> Unit,
    onShare: (Snapshot) -> Unit,
    onOpenImage: (Snapshot) -> Unit,
    onRequestDelete: (Snapshot) -> Unit,
) {
    val context = LocalContext.current
    val loadState = pagingItems.loadState.refresh
    val isInitialLoading = (loadState is LoadState.Loading && pagingItems.itemCount == 0) ||
        (uiState.isInitialLoadInProgress && pagingItems.itemCount == 0)
    val isDatabaseError = loadState is LoadState.Error && pagingItems.itemCount == 0
    val isOfflineEmpty = uiState.availabilityMode == HomeFeedAvailabilityMode.OFFLINE_EMPTY &&
        uiState.isBackgroundRefreshing.not() &&
        pagingItems.itemCount == 0
    val isEmpty = loadState is LoadState.NotLoading &&
        pagingItems.itemCount == 0 &&
        isOfflineEmpty.not()

    PullToRefreshBox(
        isRefreshing = uiState.showsRefreshIndicator,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                isInitialLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                isOfflineEmpty -> {
                    OfflineEmptyState(
                        modifier = Modifier.align(Alignment.Center),
                        onRetry = onRetry,
                    )
                }

                isDatabaseError -> {
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
                            start = 8.dp,
                            top = 8.dp,
                            end = 8.dp,
                            bottom = 8.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (uiState.showsOfflineMessaging) {
                            item(key = "home-feed-status") {
                                HomeFeedStatusPanel(
                                    uiState = uiState,
                                    onRetry = onRetry,
                                )
                            }
                        }

                        items(
                            count = pagingItems.itemCount,
                            key = { index: Int -> pagingItems[index]?.snapshotKey ?: index },
                        ) { index: Int ->
                            val snapshot = pagingItems[index] ?: return@items
                            SnapshotCard(
                                snapshot = snapshot,
                                actionPolicy = uiState.actionPolicy,
                                onReactionSelected = { emoji -> onReactionSelected(snapshot, emoji) },
                                onOpenReplies = { onOpenReplies(snapshot) },
                                onDelete = { onRequestDelete(snapshot) },
                                onShare = { onShare(snapshot) },
                                onOpenImage = { onOpenImage(snapshot) },
                                isPreview = LocalInspectionMode.current,
                                context = context,
                                currentUserId = currentUserId,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeFeedStatusPanel(
    uiState: HomeFeedUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.home_offline_banner_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.home_offline_banner_message),
                style = MaterialTheme.typography.bodyMedium,
            )
            uiState.lastSuccessfulSyncAt?.let { lastSync ->
                Text(
                    text = stringResource(
                        R.string.home_offline_last_updated,
                        formatOfflineSyncTime(lastSync),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(
                onClick = onRetry,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(text = stringResource(R.string.retry_text))
            }
        }
    }
}

@Composable
private fun SnapshotCard(
    snapshot: Snapshot,
    actionPolicy: HomeFeedActionPolicy,
    onReactionSelected: (String) -> Unit,
    onOpenReplies: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onOpenImage: () -> Unit,
    isPreview: Boolean,
    context: Context,
    currentUserId: String?,
) {
    val resources = context.resources
    val allowRemoteFallback = actionPolicy == HomeFeedActionPolicy.FULL_ACCESS
    val imageError = painterResource(R.drawable.image_error)
    val mainImageModel = if (isPreview) R.drawable.image_placeholder else snapshot.preferredPhotoModel(allowRemoteFallback)
    val avatarImageModel = if (isPreview) R.drawable.image_placeholder else snapshot.preferredUserPhotoModel(allowRemoteFallback)
    val canOpenImage = !isPreview && snapshot.hasAnyImageAvailable(allowRemoteFallback)
    val canShareImage = !isPreview && snapshot.canShareImage(allowRemoteFallback)
    val allowDelete = actionPolicy == HomeFeedActionPolicy.FULL_ACCESS
    var imageAspectRatio by remember(snapshot.snapshotKey) {
        mutableFloatStateOf(DefaultImageAspectRatio)
    }
    val replyCountLabel = formatReplyCount(resources, snapshot.replyCount)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (avatarImageModel == null) {
                    AvatarPlaceholder(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.extraLarge),
                    )
                } else {
                    SubcomposeAsyncImage(
                        model = avatarImageModel,
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
                            AvatarPlaceholder(
                                modifier = Modifier.fillMaxSize(),
                            )
                        },
                    )
                }
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
                if (snapshot.isOwnedBy(currentUserId)) {
                    IconButton(
                        onClick = onDelete,
                        enabled = allowDelete,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.home_description_button_delete),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (mainImageModel == null) {
                LimitedMediaPlaceholder()
            } else {
                SubcomposeAsyncImage(
                    model = mainImageModel,
                    contentDescription = stringResource(R.string.home_description_img_publication_user),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .aspectRatio(imageAspectRatio)
                        .clickable(
                            enabled = canOpenImage,
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
                            painter = imageError,
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
            }

            if (snapshot.isOfflineMediaPartial && actionPolicy == HomeFeedActionPolicy.READ_ONLY_OFFLINE) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_offline_media_limited),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                modifier = Modifier.padding(horizontal = 12.dp),
                text = snapshot.title.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
            )
            PostInteractionRow(
                snapshot = snapshot,
                replyCountLabel = replyCountLabel,
                canShareImage = canShareImage,
                onReactionSelected = onReactionSelected,
                onOpenReplies = onOpenReplies,
                onShare = onShare,
            )
        }
    }
}

@Composable
private fun PostInteractionRow(
    snapshot: Snapshot,
    replyCountLabel: String,
    canShareImage: Boolean,
    onReactionSelected: (String) -> Unit,
    onOpenReplies: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReactionPickerButton(
                reactionSummary = snapshot.reactionSummary,
                currentUserReaction = snapshot.currentUserReaction,
                onReactionSelected = onReactionSelected,
            )
            TextButton(
                onClick = onOpenReplies,
                modifier = Modifier
                    .testTag(ReplyButtonTag)
                    .padding(horizontal = 8.dp),
            ) {
                Text(text = replyCountLabel)
            }
            if (snapshot.hasPendingReaction || snapshot.hasPendingReply) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(16.dp),
                    strokeWidth = 2.dp,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onShare,
                enabled = canShareImage,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = stringResource(R.string.home_description_button_share),
                    tint = PrimaryLight,
                )
            }
        }
    }
}

@Composable
private fun ReactionPickerButton(
    reactionSummary: Map<String, Int>,
    currentUserReaction: String?,
    onReactionSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ReactionSummaryRow(
            summary = reactionSummary,
            currentUserReaction = currentUserReaction,
            onClick = { expanded = true },
        )
        DropdownMenu(
            shape = MaterialTheme.shapes.extraLarge,
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ReactionOptions.forEach { emoji ->
                DropdownMenuItem(
                    modifier = Modifier
                        .size(56.dp),
                    text = { Text(
                        text = emoji,
                        style = MaterialTheme.typography.titleLarge,
                    ) },
                    onClick = {
                        expanded = false
                        onReactionSelected(emoji)
                    },
                )
            }
        }
    }
}

@Composable
private fun ReactionSummaryRow(
    summary: Map<String, Int>,
    currentUserReaction: String?,
    onClick: () -> Unit,
) {
    val totalCount = summary.values.sum()

    TextButton(
        onClick = onClick,
        modifier = Modifier.testTag(ReactionButtonTag),
    ) {
        if (summary.isEmpty() || totalCount <= 0) {
            ReactionItem(
                emoji = white_hearth,
                isSelected = false,
            )
        } else {
            val sortedReactions = summary.entries
                .sortedWith(
                    compareByDescending<Map.Entry<String, Int>> { it.key == currentUserReaction }
                        .thenByDescending { it.value }
                )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sortedReactions
                    .forEach { (emoji, _) ->
                        val isSelected = emoji == currentUserReaction
                        ReactionItem(
                            emoji = emoji,
                            isSelected = isSelected,
                        )
                    }
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = "$totalCount",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
private fun ReactionItem(
    emoji: String,
    isSelected: Boolean,
) {
    val background = if (isSelected) {
        PrimaryLight.copy(alpha = 0.2f)
    } else {
        Color.Transparent
    }

    val border = if (isSelected) {
        BorderStroke(1.dp, PrimaryLight)
    } else null

    val size by animateDpAsState(
        targetValue = if (isSelected) 36.dp else 28.dp,
        label = "size"
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .then(
                if (border != null) Modifier.border(border, RoundedCornerShape(24.dp))
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            style = if (isSelected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SnapshotReplySheet(
    state: HomeReplySheetUiState,
    onDismiss: () -> Unit,
    onDraftChange: (String) -> Unit,
    onRetry: () -> Unit,
    onSubmit: () -> Unit,
) {
    val snapshot = state.snapshot ?: return
    val trimmedComposerLength = state.composerText.trim().length
    val exceedsReplyLimit = trimmedComposerLength > Constants.REPLY_CHAR_LIMIT
    val canSubmitReply = !state.isSubmitting && trimmedComposerLength in 1..Constants.REPLY_CHAR_LIMIT
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.home_reply_sheet_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = snapshot.title.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = snapshot.userName.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.errorMessageRes != null && state.replies.isEmpty() -> {
                    ReplyMessageState(
                        title = stringResource(R.string.home_reply_load_error),
                        message = stringResource(R.string.error_connectivity),
                        actionLabel = stringResource(R.string.retry_text),
                        modifier = Modifier.fillMaxWidth(),
                        onAction = onRetry,
                    )
                }

                state.isEmpty -> {
                    ReplyMessageState(
                        title = stringResource(R.string.home_reply_empty_title),
                        message = stringResource(R.string.home_reply_empty_message),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(min = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = state.replies,
                            key = SnapshotReply::replyId,
                        ) { reply ->
                            ReplyRow(reply = reply)
                        }
                    }
                }
            }

            state.errorMessageRes?.let { messageRes ->
                if (state.replies.isNotEmpty()) {
                    Text(
                        text = stringResource(messageRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            OutlinedTextField(
                value = state.composerText,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ReplyInputTag),
                label = { Text(text = stringResource(R.string.home_reply_hint)) },
                isError = exceedsReplyLimit || state.composerMessageRes != null,
                supportingText = {
                    val supportingText = if (state.composerMessageRes != null) {
                        stringResource(state.composerMessageRes)
                    } else {
                        stringResource(
                            R.string.home_reply_character_count,
                            state.composerText.length,
                            Constants.REPLY_CHAR_LIMIT,
                        )
                    }
                    Text(text = supportingText)
                },
                enabled = !state.isSubmitting,
            )

            Button(
                onClick = onSubmit,
                enabled = canSubmitReply,
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag(ReplySendButtonTag),
            ) {
                Text(
                    text = if (state.isSubmitting) {
                        stringResource(R.string.home_reply_sending)
                    } else {
                        stringResource(R.string.home_reply_send)
                    },
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReplyRow(reply: SnapshotReply) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (reply.userPhotoUrl.isNullOrBlank()) {
                AvatarPlaceholder(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(MaterialTheme.shapes.extraLarge),
                )
            } else {
                SubcomposeAsyncImage(
                    model = reply.userPhotoUrl,
                    contentDescription = stringResource(R.string.home_description_profile_user_photo),
                    modifier = Modifier
                        .size(36.dp)
                        .clip(MaterialTheme.shapes.extraLarge),
                    contentScale = ContentScale.Crop,
                    loading = {
                        ShimmerPlaceholder(
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                    error = {
                        AvatarPlaceholder(
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = reply.userName.ifBlank { stringResource(R.string.home_reply_author_fallback) },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = reply.text,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatOfflineSyncTime(reply.dateTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (reply.deliveryState == SnapshotReplyDeliveryState.PENDING) {
                        Text(
                            text = stringResource(R.string.home_reply_pending_indicator),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyMessageState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
private fun AvatarPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.extraLarge,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_person),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun LimitedMediaPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.image_placeholder),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.home_offline_media_limited),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ExpandedImageViewer(
    imageModel: Any,
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
                model = imageModel,
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
private fun OfflineEmptyState(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
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
            text = stringResource(R.string.home_offline_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.home_offline_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry_text))
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
            textAlign = TextAlign.Center,
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
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry_text))
        }
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

private fun formatOfflineSyncTime(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))

private data class ExpandedImageState(
    val imageModel: Any,
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
                idUserOwner = "preview-owner",
                userName = "Henry",
                userPhotoUrl = "",
                snapshotKey = "preview",
                reactionCount = 3,
                reactionSummary = mapOf(purple_hearth to 2, "\ud83d\udd25" to 1),
                replyCount = 2,
                currentUserReaction = purple_hearth,
            ),
            actionPolicy = HomeFeedActionPolicy.FULL_ACCESS,
            onReactionSelected = {},
            onOpenReplies = {},
            onDelete = {},
            onShare = {},
            onOpenImage = {},
            isPreview = true,
            context = LocalContext.current,
            currentUserId = "preview-owner",
        )
    }
}
