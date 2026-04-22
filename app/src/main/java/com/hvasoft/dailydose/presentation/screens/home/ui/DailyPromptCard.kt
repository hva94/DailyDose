package com.hvasoft.dailydose.presentation.screens.home.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.presentation.theme.DailyDoseTheme

@Composable
internal fun DailyPromptCard(
    promptText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.home_daily_prompt_label).uppercase(),
                color = primaryColor.copy(alpha = 0.88f),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                ),
            )

            Text(
                text = promptText,
                color = onSurfaceColor,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 36.sp,
                    lineHeight = 40.sp,
                    letterSpacing = 1.2.sp,
                ),
            )

            Text(
                text = stringResource(R.string.home_daily_prompt_helper),
                color = onSurfaceVariantColor.copy(alpha = 0.66f),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.4.sp,
                ),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.45f)
                    .drawBehind {
                        drawRoundRect(
                            color = primaryColor.copy(alpha = 0.24f),
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(4.dp.toPx(), 3.dp.toPx()),
                                ),
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                x = 24.dp.toPx(),
                                y = 24.dp.toPx(),
                            ),
                        )
                    }
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.06f),
                                primaryColor.copy(alpha = 0.045f),
                            ),
                        ),
                        shape = RoundedCornerShape(24.dp),
                    )
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_photo_camera),
                    contentDescription = stringResource(R.string.add_button_select),
                    tint = primaryColor.copy(alpha = 0.70f),
                    modifier = Modifier.size(88.dp),
                )
            }

            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .align(Alignment.CenterHorizontally)
                    .height(48.dp)
                    .width(200.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.88f),
                                primaryColor,
                                primaryColor.copy(alpha = 0.84f),
                            ),
                        ),
                        shape = RoundedCornerShape(999.dp),
                    )
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.add_button_post),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_post_arrow),
                        contentDescription = stringResource(R.string.add_button_post),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    name = "Light",
)
@Composable
private fun DailyPromptCardPreview() {
    DailyDoseTheme {
        DailyPromptCard(
            promptText = "What made today different?",
            onClick = {},
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark",
)
@Composable
private fun DailyPromptCardLongPromptPreview() {
    DailyDoseTheme {
        DailyPromptCard(
            promptText = "What was not like usual today?",
            onClick = {},
        )
    }
}
