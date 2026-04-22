package com.hvasoft.dailydose.data.config

import android.content.Context
import android.content.res.Resources
import com.google.common.truth.Truth.assertThat
import com.hvasoft.dailydose.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

class AndroidResourceDailyPromptCatalogProviderTest {

    private lateinit var context: Context
    private lateinit var resources: Resources
    private lateinit var provider: AndroidResourceDailyPromptCatalogProvider

    @Before
    fun setUp() {
        context = mockk()
        resources = mockk()
        every { context.resources } returns resources
        provider = AndroidResourceDailyPromptCatalogProvider(context)

        every { context.getString(any()) } answers {
            promptStrings[firstArg<Int>()]
                ?: throw Resources.NotFoundException("Missing string ${firstArg<Int>()}")
        }
        every { resources.getStringArray(any()) } answers {
            arrayEntries[firstArg<Int>()]
                ?: throw Resources.NotFoundException("Missing array ${firstArg<Int>()}")
        }
    }

    @Test
    fun `getDailyPromptCombos maps all seven combos from resources`() {
        val combos = provider.getDailyPromptCombos()

        assertThat(combos).hasSize(7)
        expectedCombos.forEachIndexed { index, expectedCombo ->
            assertThat(combos[index].comboId).isEqualTo(expectedCombo.comboId)
            assertThat(combos[index].promptText).isEqualTo(expectedCombo.promptText)
            assertThat(combos[index].titlePatterns)
                .containsExactlyElementsIn(expectedCombo.titlePatterns)
                .inOrder()
            assertThat(combos[index].answerFormats)
                .containsExactlyElementsIn(expectedCombo.answerFormats)
                .inOrder()
        }
    }

    @Test
    fun `getDailyPromptCombos fails closed when a required array is missing`() {
        every { resources.getStringArray(R.array.daily_prompt_combo_4_title_patterns) } throws Resources.NotFoundException()

        val combos = provider.getDailyPromptCombos()

        assertThat(combos).isEmpty()
    }

    private companion object {
        val sharedAnswerFormats = listOf(
            "{answer} · %time",
            "{answer} at %time",
        )

        val expectedCombos = listOf(
            ExpectedCombo(
                comboId = "daily-prompt-1",
                promptText = "What made today different?",
                titlePatterns = listOf("Today felt different at %time", "A different moment at %time"),
            ),
            ExpectedCombo(
                comboId = "daily-prompt-2",
                promptText = "What stood out today?",
                titlePatterns = listOf("This stood out at %time", "Something stood out at %time"),
            ),
            ExpectedCombo(
                comboId = "daily-prompt-3",
                promptText = "What changed today?",
                titlePatterns = listOf("Something changed at %time", "A change at %time"),
            ),
            ExpectedCombo(
                comboId = "daily-prompt-4",
                promptText = "What caught your attention today?",
                titlePatterns = listOf("This caught my attention at %time", "Caught this at %time"),
            ),
            ExpectedCombo(
                comboId = "daily-prompt-5",
                promptText = "What felt different today?",
                titlePatterns = listOf("Felt different at %time", "This felt different at %time"),
            ),
            ExpectedCombo(
                comboId = "daily-prompt-6",
                promptText = "What was not like usual today?",
                titlePatterns = listOf("Not like usual at %time", "Something unusual at %time"),
            ),
            ExpectedCombo(
                comboId = "daily-prompt-7",
                promptText = "What broke the routine today?",
                titlePatterns = listOf("Broke the routine at %time", "Out of routine at %time"),
            ),
        )

        val promptStrings = mapOf(
            R.string.daily_prompt_combo_1_prompt to "What made today different?",
            R.string.daily_prompt_combo_2_prompt to "What stood out today?",
            R.string.daily_prompt_combo_3_prompt to "What changed today?",
            R.string.daily_prompt_combo_4_prompt to "What caught your attention today?",
            R.string.daily_prompt_combo_5_prompt to "What felt different today?",
            R.string.daily_prompt_combo_6_prompt to "What was not like usual today?",
            R.string.daily_prompt_combo_7_prompt to "What broke the routine today?",
        )

        val arrayEntries = mapOf(
            R.array.daily_prompt_answer_formats to sharedAnswerFormats.toTypedArray(),
            R.array.daily_prompt_combo_1_title_patterns to arrayOf(
                "Today felt different at %time",
                "A different moment at %time",
            ),
            R.array.daily_prompt_combo_2_title_patterns to arrayOf(
                "This stood out at %time",
                "Something stood out at %time",
            ),
            R.array.daily_prompt_combo_3_title_patterns to arrayOf(
                "Something changed at %time",
                "A change at %time",
            ),
            R.array.daily_prompt_combo_4_title_patterns to arrayOf(
                "This caught my attention at %time",
                "Caught this at %time",
            ),
            R.array.daily_prompt_combo_5_title_patterns to arrayOf(
                "Felt different at %time",
                "This felt different at %time",
            ),
            R.array.daily_prompt_combo_6_title_patterns to arrayOf(
                "Not like usual at %time",
                "Something unusual at %time",
            ),
            R.array.daily_prompt_combo_7_title_patterns to arrayOf(
                "Broke the routine at %time",
                "Out of routine at %time",
            ),
        )
    }

    private data class ExpectedCombo(
        val comboId: String,
        val promptText: String,
        val titlePatterns: List<String>,
        val answerFormats: List<String> = sharedAnswerFormats,
    )
}
