package com.hvasoft.dailydose.presentation.screens.home.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SnapshotCardReactionBehaviorTest {

    @Test
    fun `quick reaction selects purple heart when no reaction exists`() {
        assertThat(resolveQuickReactionSelection(currentUserReaction = null))
            .isEqualTo(PurpleHeartEmoji)
    }

    @Test
    fun `quick reaction clears purple heart when it is already selected`() {
        assertThat(resolveQuickReactionSelection(currentUserReaction = PurpleHeartEmoji))
            .isNull()
    }

    @Test
    fun `quick reaction clears any existing non default reaction`() {
        assertThat(resolveQuickReactionSelection(currentUserReaction = "\uD83D\uDD25"))
            .isNull()
    }
}
