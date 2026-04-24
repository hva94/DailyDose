package com.hvasoft.dailydose.presentation.screens.home

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.model.SnapshotRevealSyncState
import com.hvasoft.dailydose.domain.model.SnapshotVisibilityMode
import com.hvasoft.dailydose.presentation.screens.home.ui.SnapshotCard
import com.hvasoft.dailydose.presentation.screens.home.ui.SnapshotRevealOverlayTag
import com.hvasoft.dailydose.presentation.theme.DailyDoseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SnapshotRevealTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun ownerSnapshotNeverShowsRevealOverlay() {
        val imagePath = createLocalImageFile("snapshot-reveal-owner.jpg")

        composeRule.setContent {
            val context = LocalContext.current
            DailyDoseTheme {
                SnapshotCard(
                    snapshot = Snapshot(
                        snapshotKey = "snapshot-owner",
                        title = "Owner post",
                        idUserOwner = "viewer-1",
                        userName = "Henry",
                        localPhotoPath = imagePath,
                        visibilityMode = SnapshotVisibilityMode.VISIBLE_OWNER,
                        revealSyncState = SnapshotRevealSyncState.CONFIRMED,
                        isRevealedForViewer = true,
                        isOwnerView = true,
                    ),
                    actionPolicy = HomeFeedActionPolicy.FULL_ACCESS,
                    onReactionSelected = {},
                    onOpenReplies = {},
                    onDelete = {},
                    onShare = {},
                    onOpenImage = {},
                    isPreview = false,
                    context = context,
                    currentUserId = "viewer-1",
                )
            }
        }

        assertEquals(
            0,
            composeRule.onAllNodesWithTag(SnapshotRevealOverlayTag).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun hiddenSnapshotShowsRevealOverlay() {
        val imagePath = createLocalImageFile("snapshot-reveal-hidden.jpg")

        composeRule.setContent {
            val context = LocalContext.current
            DailyDoseTheme {
                SnapshotCard(
                    snapshot = hiddenSnapshot(imagePath),
                    actionPolicy = HomeFeedActionPolicy.FULL_ACCESS,
                    onReactionSelected = {},
                    onOpenReplies = {},
                    onDelete = {},
                    onShare = {},
                    onOpenImage = {},
                    isPreview = false,
                    context = context,
                    currentUserId = "viewer-1",
                )
            }
        }

        assertEquals(
            1,
            composeRule
                .onAllNodesWithTag(SnapshotRevealOverlayTag, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size,
        )
    }

    private fun hiddenSnapshot(imagePath: String): Snapshot = Snapshot(
        snapshotKey = "snapshot-hidden",
        title = "Hidden post",
        idUserOwner = "owner-1",
        userName = "Alex",
        localPhotoPath = imagePath,
        visibilityMode = SnapshotVisibilityMode.HIDDEN_UNREVEALED,
        revealSyncState = SnapshotRevealSyncState.NONE,
        isRevealedForViewer = false,
        isOwnerView = false,
    )

    private fun createLocalImageFile(name: String): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, name)
        file.writeText("fake-image")
        return file.absolutePath
    }
}
