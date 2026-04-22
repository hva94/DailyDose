package com.hvasoft.dailydose.data.config

import android.content.Context
import android.content.res.Resources
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.domain.model.DailyPromptCombo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidResourceDailyPromptCatalogProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : DailyPromptCatalogProvider {

    override fun getDailyPromptCombos(): List<DailyPromptCombo> = try {
        val resources = context.resources
        val answerFormats = resources.readEntries(
            arrayResId = R.array.daily_prompt_answer_formats,
            requiredSize = REQUIRED_ANSWER_FORMAT_COUNT,
        )
        if (answerFormats == null) {
            emptyList()
        } else {
            comboSpecs.map { spec ->
                val promptText = context.getString(spec.promptResId)
                    .trim()
                    .takeIf(String::isNotBlank)
                    ?: return emptyList()
                val titlePatterns = resources.readEntries(
                    arrayResId = spec.titlePatternsResId,
                    requiredSize = REQUIRED_TITLE_PATTERN_COUNT,
                ) ?: return emptyList()

                DailyPromptCombo(
                    comboId = spec.comboId,
                    promptText = promptText,
                    titlePatterns = titlePatterns,
                    answerFormats = answerFormats,
                )
            }
        }
    } catch (_: Resources.NotFoundException) {
        emptyList()
    }

    private fun Resources.readEntries(
        @ArrayRes arrayResId: Int,
        requiredSize: Int,
    ): List<String>? {
        val entries = getStringArray(arrayResId)
            .map(String::trim)
            .filter(String::isNotBlank)
        return entries.takeIf { it.size == requiredSize }
    }

    private data class ComboResourceSpec(
        val comboId: String,
        @StringRes val promptResId: Int,
        @ArrayRes val titlePatternsResId: Int,
    )

    private companion object {
        const val REQUIRED_TITLE_PATTERN_COUNT = 2
        const val REQUIRED_ANSWER_FORMAT_COUNT = 2

        val comboSpecs = listOf(
            ComboResourceSpec(
                comboId = "daily-prompt-1",
                promptResId = R.string.daily_prompt_combo_1_prompt,
                titlePatternsResId = R.array.daily_prompt_combo_1_title_patterns,
            ),
            ComboResourceSpec(
                comboId = "daily-prompt-2",
                promptResId = R.string.daily_prompt_combo_2_prompt,
                titlePatternsResId = R.array.daily_prompt_combo_2_title_patterns,
            ),
            ComboResourceSpec(
                comboId = "daily-prompt-3",
                promptResId = R.string.daily_prompt_combo_3_prompt,
                titlePatternsResId = R.array.daily_prompt_combo_3_title_patterns,
            ),
            ComboResourceSpec(
                comboId = "daily-prompt-4",
                promptResId = R.string.daily_prompt_combo_4_prompt,
                titlePatternsResId = R.array.daily_prompt_combo_4_title_patterns,
            ),
            ComboResourceSpec(
                comboId = "daily-prompt-5",
                promptResId = R.string.daily_prompt_combo_5_prompt,
                titlePatternsResId = R.array.daily_prompt_combo_5_title_patterns,
            ),
            ComboResourceSpec(
                comboId = "daily-prompt-6",
                promptResId = R.string.daily_prompt_combo_6_prompt,
                titlePatternsResId = R.array.daily_prompt_combo_6_title_patterns,
            ),
            ComboResourceSpec(
                comboId = "daily-prompt-7",
                promptResId = R.string.daily_prompt_combo_7_prompt,
                titlePatternsResId = R.array.daily_prompt_combo_7_title_patterns,
            ),
        )
    }
}
