package com.hvasoft.dailydose.presentation.screens.home.ui

import android.content.Context
import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.domain.common.extension_functions.canOpenExpandedImage
import com.hvasoft.dailydose.domain.common.extension_functions.canRevealImage
import com.hvasoft.dailydose.domain.common.extension_functions.canUseInteractions
import com.hvasoft.dailydose.domain.common.extension_functions.isOwnedBy
import com.hvasoft.dailydose.domain.common.extension_functions.isHiddenFromViewer
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.model.SnapshotVisibilityMode
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
internal const val PurpleHeartEmoji = "\uD83D\uDC9C"
private val ReactionOptions = listOf(
    PurpleHeartEmoji,
    "\uD83D\uDD25",
    "\uD83D\uDE02",
    "\uD83D\uDE2E",
    "\uD83D\uDE22",
    "\uD83D\uDE21",
)

internal const val ReactionButtonTag = "snapshot_reaction_button"
internal const val ReplyButtonTag = "snapshot_reply_button"
internal const val ShareButtonTag = "snapshot_share_button"
internal const val SnapshotImageTag = "snapshot_image_area"
internal const val SnapshotRevealOverlayTag = "snapshot_reveal_overlay"

@Composable
internal fun SnapshotCard(
    snapshot: Snapshot,
    actionPolicy: HomeFeedActionPolicy,
    onReactionSelected: (String?) -> Unit,
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
    val isHiddenFromViewer = snapshot.isHiddenFromViewer(currentUserId)
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
    val canRevealImage = !isPreview &&
        mainImageModel != null &&
        snapshot.canRevealImage(currentUserId)
    val canOpenImage = !isPreview &&
        snapshot.hasAnyImageAvailable(allowRemoteFallback) &&
        snapshot.canOpenExpandedImage(currentUserId)
    val canUseInteractions = snapshot.canUseInteractions(currentUserId)
    val canShareImage = !isPreview && snapshot.canShareImage(allowRemoteFallback, currentUserId)
    val allowDelete = actionPolicy == HomeFeedActionPolicy.FULL_ACCESS
    var imageAspectRatio by remember(snapshot.snapshotKey) {
        mutableFloatStateOf(DefaultImageAspectRatio)
    }
    var hasRevealStarted by remember(snapshot.snapshotKey, currentUserId) {
        mutableStateOf(snapshot.visibilityMode == SnapshotVisibilityMode.VISIBLE_OWNER ||
            snapshot.visibilityMode == SnapshotVisibilityMode.VISIBLE_REVEALED)
    }
    val shouldShowHiddenTreatment = isHiddenFromViewer && !hasRevealStarted
    val blurRadius by animateDpAsState(
        targetValue = if (shouldShowHiddenTreatment) {
            Constants.SNAPSHOT_REVEAL_BLUR_DP.dp
        } else {
            0.dp
        },
        label = "snapshot_reveal_blur",
    )
    val overlayAlpha by animateFloatAsState(
        targetValue = if (shouldShowHiddenTreatment) {
            Constants.SNAPSHOT_REVEAL_OVERLAY_ALPHA
        } else {
            0f
        },
        label = "snapshot_reveal_overlay",
    )
    val imageContentDescription = if (shouldShowHiddenTreatment) {
        stringResource(R.string.home_reveal_hidden_image_description)
    } else {
        stringResource(R.string.home_description_img_publication_user)
    }
    val revealLabel = stringResource(R.string.home_reveal_tap_to_reveal)
    val imageRenderMode = resolveSnapshotRevealImageRenderMode(
        shouldShowHiddenTreatment = shouldShowHiddenTreatment,
        sdkInt = Build.VERSION.SDK_INT,
    )
    val resolvedMainImageModel = remember(context, mainImageModel, imageRenderMode) {
        buildSnapshotRevealImageModel(
            context = context,
            originalModel = mainImageModel,
            renderMode = imageRenderMode,
        )
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .aspectRatio(imageAspectRatio)
                    .testTag(SnapshotImageTag)
                    .semantics {
                        contentDescription = imageContentDescription
                        if (shouldShowHiddenTreatment) {
                            stateDescription = revealLabel
                        }
                    }
                    .clickable(
                        enabled = canRevealImage || canOpenImage,
                        role = Role.Button,
                        onClick = {
                            if (canRevealImage) {
                                hasRevealStarted = true
                            }
                            onOpenImage()
                        },
                    ),
            ) {
                if (resolvedMainImageModel == null) {
                    LimitedMediaPlaceholder(
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    SubcomposeAsyncImage(
                        model = resolvedMainImageModel,
                        contentDescription = imageContentDescription,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (imageRenderMode == SnapshotRevealImageRenderMode.ComposeBlur) {
                                    Modifier.blur(blurRadius)
                                } else {
                                    Modifier
                                },
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
                                contentDescription = imageContentDescription,
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
                if (overlayAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = overlayAlpha * 0.24f))
                            .testTag(SnapshotRevealOverlayTag),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = revealLabel,
                            modifier = Modifier.graphicsLayer { alpha = overlayAlpha },
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp,
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
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
            SnapshotCopy(
                promptText = snapshot.dailyPromptText,
                title = snapshot.title.orEmpty(),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            PostInteractionRow(
                snapshot = snapshot,
                replyCount = snapshot.replyCount,
                interactionsEnabled = canUseInteractions,
                canShareImage = canShareImage,
                onReactionSelected = onReactionSelected,
                onOpenReplies = onOpenReplies,
                onShare = onShare,
            )
        }
    }
}

@Composable
private fun SnapshotCopy(
    promptText: String?,
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        promptText
            ?.takeIf(String::isNotBlank)
            ?.let { resolvedPromptText ->
                Text(
                    text = resolvedPromptText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        ExpandableSnapshotTitle(
            title = title,
        )
    }
}

@Composable
private fun ExpandableSnapshotTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember(title) { mutableStateOf(false) }
    var canExpand by remember(title) { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = if (isExpanded) Int.MAX_VALUE else Constants.SNAPSHOT_TITLE_COLLAPSED_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult ->
                if (!isExpanded) {
                    canExpand = textLayoutResult.hasVisualOverflow
                }
            },
        )
        if (canExpand || isExpanded) {
            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = if (isExpanded) {
                        stringResource(R.string.home_title_collapse)
                    } else {
                        stringResource(R.string.home_title_expand)
                    },
                )
            }
        }
    }
}

@Composable
private fun PostInteractionRow(
    snapshot: Snapshot,
    replyCount: Int,
    interactionsEnabled: Boolean,
    canShareImage: Boolean,
    onReactionSelected: (String?) -> Unit,
    onOpenReplies: () -> Unit,
    onShare: () -> Unit,
) {
    val replyCountLabel = if (replyCount > 0) {
        replyCount.toString()
    } else {
        ""
    }
    val interactionTint = if (interactionsEnabled) {
        PrimaryLight
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
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
                enabled = interactionsEnabled,
                tint = interactionTint,
                onReactionSelected = onReactionSelected,
            )
            TextButton(
                onClick = onOpenReplies,
                enabled = interactionsEnabled,
                modifier = Modifier.testTag(ReplyButtonTag),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_comment),
                        contentDescription = stringResource(R.string.home_description_button_reply),
                        tint = interactionTint,
                    )
                    Text(
                        text = replyCountLabel,
                        modifier = Modifier.padding(start = 4.dp),
                        color = interactionTint,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
            if (snapshot.hasPendingReaction || snapshot.hasPendingReply || snapshot.hasPendingRevealSync) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onShare,
                enabled = canShareImage,
                modifier = Modifier.testTag(ShareButtonTag),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = stringResource(R.string.home_description_button_share_action),
                    tint = if (canShareImage) PrimaryLight else interactionTint,
                )
            }
        }
    }
}

@Composable
private fun ReactionPickerButton(
    reactionSummary: Map<String, Int>,
    currentUserReaction: String?,
    enabled: Boolean,
    tint: Color,
    onReactionSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ReactionSummaryRow(
            summary = reactionSummary,
            currentUserReaction = currentUserReaction,
            enabled = enabled,
            tint = tint,
            onClick = {
                onReactionSelected(resolveQuickReactionSelection(currentUserReaction))
            },
            onLongClick = { expanded = true },
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
@OptIn(ExperimentalFoundationApi::class)
private fun ReactionSummaryRow(
    summary: Map<String, Int>,
    currentUserReaction: String?,
    enabled: Boolean,
    tint: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val totalCount = summary.values.sum()
    val longClickLabel = stringResource(R.string.home_reaction_picker_action)

    Box(
        modifier = Modifier,
    ) {
        Row(
            modifier = Modifier
                .testTag(ReactionButtonTag)
                .combinedClickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onLongClickLabel = longClickLabel,
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (summary.isEmpty() || totalCount <= 0) {
                ReactionItem(
                    emoji = WhiteHeartEmoji,
                    isSelected = false,
                    enabled = enabled,
                    tint = tint,
                )
            } else {
                val sortedReactions = summary.entries
                    .sortedWith(
                        compareByDescending<Map.Entry<String, Int>> { it.key == currentUserReaction }
                            .thenByDescending { it.value },
                    )

                sortedReactions.forEach { (emoji, _) ->
                    ReactionItem(
                        emoji = emoji,
                        isSelected = emoji == currentUserReaction,
                        enabled = enabled,
                        tint = tint,
                    )
                }
                Text(
                    text = "$totalCount",
                    modifier = Modifier.padding(start = 4.dp),
                    color = tint,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
    }
}

internal fun resolveQuickReactionSelection(currentUserReaction: String?): String? =
    if (currentUserReaction.isNullOrBlank()) {
        PurpleHeartEmoji
    } else {
        null
    }

@Composable
private fun ReactionItem(
    emoji: String,
    isSelected: Boolean,
    enabled: Boolean,
    tint: Color,
) {
    val background = if (!enabled) {
        tint.copy(alpha = if (isSelected) 0.18f else 0.1f)
    } else if (isSelected) {
        PrimaryLight.copy(alpha = 0.2f)
    } else {
        Color.Transparent
    }
    val border = if (!enabled && isSelected) {
        BorderStroke(1.dp, tint.copy(alpha = 0.5f))
    } else if (isSelected) {
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
            .alpha(if (enabled) 1f else 0.45f)
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
            color = if (enabled) Color.Unspecified else tint,
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
                dailyPromptText = "What stood out today?",
                userName = "Henry",
                userPhotoUrl = "",
                snapshotKey = "preview",
                reactionCount = 3,
                reactionSummary = mapOf(PurpleHeartEmoji to 2, "\uD83D\uDD25" to 1),
                replyCount = 2,
                currentUserReaction = PurpleHeartEmoji,
                visibilityMode = SnapshotVisibilityMode.VISIBLE_OWNER,
                isOwnerView = true,
                isRevealedForViewer = true,
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
