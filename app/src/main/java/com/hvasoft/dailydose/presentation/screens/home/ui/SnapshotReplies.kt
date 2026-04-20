package com.hvasoft.dailydose.presentation.screens.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.domain.model.SnapshotReply
import com.hvasoft.dailydose.domain.model.SnapshotReplyDeliveryState
import com.hvasoft.dailydose.presentation.screens.common.ShimmerPlaceholder
import com.hvasoft.dailydose.presentation.screens.home.HomeReplySheetUiState

private const val ReplyInputTag = "snapshot_reply_input"
private const val ReplySendButtonTag = "snapshot_reply_send_button"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SnapshotReplySheet(
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
                state.isLoading && state.replies.isEmpty() -> {
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
                        if (state.isLoading) {
                            item(key = "reply_loading_indicator") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }
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
                    text = reply.userName.ifBlank {
                        stringResource(R.string.home_reply_author_fallback)
                    },
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
