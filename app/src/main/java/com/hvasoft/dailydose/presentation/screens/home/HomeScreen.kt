package com.hvasoft.dailydose.presentation.screens.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.domain.common.extension_functions.canOpenExpandedImage
import com.hvasoft.dailydose.domain.common.extension_functions.canRevealImage
import com.hvasoft.dailydose.domain.model.DailyPromptAssignment
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.presentation.screens.home.ui.ExpandedImageState
import com.hvasoft.dailydose.presentation.screens.home.ui.ExpandedImageViewer
import com.hvasoft.dailydose.presentation.screens.home.ui.HomeContent
import com.hvasoft.dailydose.presentation.screens.home.ui.SnapshotReplySheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    scrollSignal: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onShowMessage: (Int) -> Unit,
    onOpenDailyPrompt: (DailyPromptAssignment) -> Unit,
) {
    val pagingItems = viewModel.snapshots.collectAsLazyPagingItems()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val replySheetState by viewModel.replySheetState.collectAsStateWithLifecycle()
    val postPublishScrollSignal by viewModel.postPublishScrollSignal.collectAsStateWithLifecycle()
    var pendingDeleteSnapshot by remember { mutableStateOf<Snapshot?>(null) }
    var expandedImage by remember { mutableStateOf<ExpandedImageState?>(null) }
    var shouldScrollToTop by remember { mutableStateOf(false) }
    var lastConsumedPostPublishScrollSignal by rememberSaveable { mutableIntStateOf(0) }
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

    LaunchedEffect(postPublishScrollSignal) {
        if (
            postPublishScrollSignal == 0 ||
            postPublishScrollSignal == lastConsumedPostPublishScrollSignal
        ) {
            return@LaunchedEffect
        }
        shouldScrollToTop = true
        lastConsumedPostPublishScrollSignal = postPublishScrollSignal
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

    HomeContent(
        pagingItems = pagingItems,
        uiState = uiState,
        listState = listState,
        currentUserId = currentUserId,
        contentPadding = contentPadding,
        modifier = modifier,
        onRetry = viewModel::retrySync,
        onRefresh = viewModel::retrySync,
        onOpenDailyPrompt = { uiState.activeDailyPrompt?.let(onOpenDailyPrompt) },
        onReactionSelected = viewModel::setSnapshotReaction,
        onOpenReplies = viewModel::openReplies,
        onShare = { snapshot ->
            shareSnapshotIfAvailable(
                context = context,
                snapshot = snapshot,
                hasFullAccess = uiState.allowRemoteMediaFallback,
                currentUserId = currentUserId,
                onShowMessage = onShowMessage,
                launch = { block ->
                    scope.launch {
                        block()
                    }
                },
            )
        },
        onImageTap = { snapshot ->
            if (snapshot.canRevealImage(currentUserId)) {
                viewModel.revealSnapshot(snapshot)
                return@HomeContent
            }
            if (!snapshot.canOpenExpandedImage(currentUserId)) {
                return@HomeContent
            }
            val imageModel = snapshot.preferredPhotoModel(
                allowRemoteFallback = uiState.allowRemoteMediaFallback,
            )
            if (imageModel == null) {
                onShowMessage(R.string.home_image_unavailable_offline)
                return@HomeContent
            }

            expandedImage =
                ExpandedImageState(
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

    pendingDeleteSnapshot?.let { snapshot ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSnapshot = null },
            title = { Text(text = stringResource(R.string.dialog_delete_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSnapshot(snapshot)
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
