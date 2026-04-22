package com.hvasoft.dailydose.presentation.screens.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.domain.model.HomeFeedAvailabilityMode
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.presentation.screens.home.HomeFeedUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeContent(
    pagingItems: LazyPagingItems<Snapshot>,
    uiState: HomeFeedUiState,
    listState: LazyListState,
    currentUserId: String?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onOpenDailyPrompt: () -> Unit,
    onReactionSelected: (Snapshot, String?) -> Unit,
    onOpenReplies: (Snapshot) -> Unit,
    onShare: (Snapshot) -> Unit,
    onImageTap: (Snapshot) -> Unit,
    onRequestDelete: (Snapshot) -> Unit,
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val loadState = pagingItems.loadState.refresh
    val isInitialLoading = (loadState is LoadState.Loading && pagingItems.itemCount == 0) ||
        (uiState.isInitialLoadInProgress && pagingItems.itemCount == 0)
    val isDatabaseError = loadState is LoadState.Error && pagingItems.itemCount == 0
    val isOfflineEmpty = uiState.availabilityMode == HomeFeedAvailabilityMode.OFFLINE_EMPTY &&
        uiState.isBackgroundRefreshing.not() &&
        pagingItems.itemCount == 0
    val shouldDeferListUntilPromptLoads = uiState.isPromptLoading &&
        loadState is LoadState.NotLoading &&
        pagingItems.itemCount > 0
    val isEmpty = loadState is LoadState.NotLoading &&
        pagingItems.itemCount == 0 &&
        isOfflineEmpty.not() &&
        uiState.shouldShowDailyPromptCard.not()
    val listContentPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(layoutDirection) + 8.dp,
        top = contentPadding.calculateTopPadding() + 8.dp,
        end = contentPadding.calculateEndPadding(layoutDirection) + 8.dp,
        bottom = contentPadding.calculateBottomPadding() + 8.dp,
    )

    PullToRefreshBox(
        isRefreshing = uiState.showsRefreshIndicator,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                isInitialLoading || shouldDeferListUntilPromptLoads -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(contentPadding),
                    )
                }

                isOfflineEmpty -> {
                    OfflineEmptyState(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(contentPadding),
                        onRetry = onRetry,
                    )
                }

                isDatabaseError -> {
                    ErrorState(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(contentPadding),
                        onRetry = onRetry,
                    )
                }

                isEmpty -> {
                    EmptyState(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(contentPadding),
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = listContentPadding,
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
                        if (uiState.shouldShowDailyPromptCard) {
                            item(key = "daily-prompt-card") {
                                DailyPromptCard(
                                    promptText = uiState.activeDailyPrompt?.promptText.orEmpty(),
                                    onClick = onOpenDailyPrompt,
                                )
                            }
                        }

                        items(
                            count = pagingItems.itemCount,
                            key = { index -> pagingItems[index]?.snapshotKey ?: index },
                        ) { index ->
                            val snapshot = pagingItems[index] ?: return@items
                            SnapshotCard(
                                snapshot = snapshot,
                                actionPolicy = uiState.actionPolicy,
                                onReactionSelected = { emoji ->
                                    onReactionSelected(
                                        snapshot,
                                        emoji
                                    )
                                },
                                onOpenReplies = { onOpenReplies(snapshot) },
                                onDelete = { onRequestDelete(snapshot) },
                                onShare = { onShare(snapshot) },
                                onOpenImage = { onImageTap(snapshot) },
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
