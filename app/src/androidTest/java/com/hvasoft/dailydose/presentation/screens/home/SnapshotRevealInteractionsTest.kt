package com.hvasoft.dailydose.presentation.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.model.SnapshotRevealSyncState
import com.hvasoft.dailydose.domain.model.SnapshotVisibilityMode
import com.hvasoft.dailydose.presentation.screens.home.ui.ReactionButtonTag
import com.hvasoft.dailydose.presentation.screens.home.ui.ReplyButtonTag
import com.hvasoft.dailydose.presentation.screens.home.ui.ShareButtonTag
import com.hvasoft.dailydose.presentation.screens.home.ui.SnapshotCard
import com.hvasoft.dailydose.presentation.screens.home.ui.SnapshotImageTag
import com.hvasoft.dailydose.presentation.screens.home.ui.SnapshotRevealOverlayTag
import com.hvasoft.dailydose.presentation.theme.DailyDoseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SnapshotRevealInteractionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hiddenSnapshotDisablesControlsUntilRevealCompletes() {
        val imagePath = createLocalImageFile("snapshot-reveal-actions.jpg")

        composeRule.setContent {
            var snapshot by remember { mutableStateOf(hiddenSnapshot(imagePath)) }
            val context = LocalContext.current

            DailyDoseTheme {
                SnapshotCard(
                    snapshot = snapshot,
                    actionPolicy = HomeFeedActionPolicy.FULL_ACCESS,
                    onReactionSelected = {},
                    onOpenReplies = {},
                    onDelete = {},
                    onShare = {},
                    onOpenImage = {
                        snapshot = snapshot.copy(
                            visibilityMode = SnapshotVisibilityMode.VISIBLE_REVEALED,
                            revealSyncState = SnapshotRevealSyncState.PENDING,
                            isRevealedForViewer = true,
                        )
                    },
                    isPreview = false,
                    context = context,
                    currentUserId = "viewer-1",
                )
            }
        }

        composeRule.onNodeWithTag(ReactionButtonTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(ReplyButtonTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(ShareButtonTag).assertIsNotEnabled()

        composeRule.onNodeWithTag(SnapshotImageTag).performClick()
        composeRule.waitForIdle()

        assertEquals(
            0,
            composeRule.onAllNodesWithTag(SnapshotRevealOverlayTag).fetchSemanticsNodes().size,
        )
        composeRule.onNodeWithTag(ReactionButtonTag).assertIsEnabled()
        composeRule.onNodeWithTag(ReplyButtonTag).assertIsEnabled()
        composeRule.onNodeWithTag(ShareButtonTag).assertIsEnabled()
    }

    private fun hiddenSnapshot(imagePath: String): Snapshot = Snapshot(
        snapshotKey = "snapshot-hidden-actions",
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
