package com.hvasoft.dailydose.presentation.screens.home.ui

import android.content.Context
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.domain.common.extension_functions.isOwnedBy
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.presentation.screens.common.DefaultImageAspectRatio
import com.hvasoft.dailydose.presentation.screens.common.ShimmerPlaceholder
import com.hvasoft.dailydose.presentation.screens.common.calculateClampedAspectRatio
import com.hvasoft.dailydose.presentation.screens.common.formatRelativeTime
import com.hvasoft.dailydose.presentation.screens.home.HomeFeedActionPolicy
import com.hvasoft.dailydose.presentation.screens.home.canShareImage
import com.hvasoft.dailydose.presentation.theme.DailyDoseTheme
import com.hvasoft.dailydose.presentation.theme.PrimaryLight
import com.hvasoft.dailydose.presentation.theme.Unselected

private const val WhiteHeartEmoji = "\uD83E\uDD0D"
private const val PurpleHeartEmoji = "\uD83D\uDC9C"
private val ReactionOptions = listOf(
    PurpleHeartEmoji,
    "\uD83D\uDD25",
    "\uD83D\uDE02",
    "\uD83D\uDE2E",
    "\uD83D\uDE22",
    "\uD83D\uDE21",
)

private const val ReactionButtonTag = "snapshot_reaction_button"
private const val ReplyButtonTag = "snapshot_reply_button"

@Composable
internal fun SnapshotCard(
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
    val mainImageModel = if (isPreview) {
        R.drawable.image_placeholder
    } else {
        snapshot.preferredPhotoModel(allowRemoteFallback)
    }
    val avatarImageModel = if (isPreview) {
        R.drawable.image_placeholder
    } else {
        snapshot.preferredUserPhotoModel(allowRemoteFallback)
    }
    val canOpenImage = !isPreview && snapshot.hasAnyImageAvailable(allowRemoteFallback)
    val canShareImage = !isPreview && snapshot.canShareImage(allowRemoteFallback)
    val allowDelete = actionPolicy == HomeFeedActionPolicy.FULL_ACCESS
    var imageAspectRatio by remember(snapshot.snapshotKey) {
        mutableFloatStateOf(DefaultImageAspectRatio)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 8.dp),
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
                            tint = Unselected,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                replyCount = snapshot.replyCount,
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
    replyCount: Int,
    canShareImage: Boolean,
    onReactionSelected: (String) -> Unit,
    onOpenReplies: () -> Unit,
    onShare: () -> Unit,
) {
    val replyCountLabel = if (replyCount > 0) {
        replyCount.toString()
    } else {
        ""
    }

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
                modifier = Modifier.testTag(ReplyButtonTag),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_comment),
                        contentDescription = stringResource(R.string.home_description_button_share),
                        tint = PrimaryLight,
                    )
                    Text(text = replyCountLabel)
                }
            }
            if (snapshot.hasPendingReaction || snapshot.hasPendingReply) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
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
                    modifier = Modifier.size(56.dp),
                    text = {
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
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
                emoji = WhiteHeartEmoji,
                isSelected = false,
            )
        } else {
            val sortedReactions = summary.entries
                .sortedWith(
                    compareByDescending<Map.Entry<String, Int>> { it.key == currentUserReaction }
                        .thenByDescending { it.value },
                )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                sortedReactions.forEach { (emoji, _) ->
                    ReactionItem(
                        emoji = emoji,
                        isSelected = emoji == currentUserReaction,
                    )
                }
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = "$totalCount",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
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
    } else {
        null
    }
    val size by animateDpAsState(
        targetValue = if (isSelected) 36.dp else 28.dp,
        label = "size",
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .then(
                if (border != null) {
                    Modifier.border(border, RoundedCornerShape(24.dp))
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SnapshotCardPreview() {
    DailyDoseTheme {
        SnapshotCard(
            snapshot = Snapshot(
                title = "My Daily Dose at 12:00 p. m.",
                dateTime = System.currentTimeMillis() - 3_600_000,
                photoUrl = "",
                idUserOwner = "preview-owner",
                userName = "Henry",
                userPhotoUrl = "",
                snapshotKey = "preview",
                reactionCount = 3,
                reactionSummary = mapOf(PurpleHeartEmoji to 2, "\uD83D\uDD25" to 1),
                replyCount = 2,
                currentUserReaction = PurpleHeartEmoji,
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
