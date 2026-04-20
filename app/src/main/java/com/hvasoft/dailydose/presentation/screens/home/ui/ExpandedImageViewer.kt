package com.hvasoft.dailydose.presentation.screens.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.presentation.screens.common.ShimmerPlaceholder

@Composable
internal fun ExpandedImageViewer(
    imageModel: Any,
    title: String,
    onDismiss: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val imagePlaceholder = painterResource(R.drawable.image_placeholder)

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = newScale
        offset = if (newScale > 1f) {
            clampOffset(offset + panChange, newScale, containerSize)
        } else {
            Offset.Zero
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp)
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                val zoomScale = 2.5f
                                scale = zoomScale
                                val centeredOffset = Offset(
                                    x = (containerSize.width / 2f - tapOffset.x) * (zoomScale - 1f),
                                    y = (containerSize.height / 2f - tapOffset.y) * (zoomScale - 1f),
                                )
                                offset = clampOffset(centeredOffset, zoomScale, containerSize)
                            }
                        },
                    )
                },
        ) {
            SubcomposeAsyncImage(
                model = imageModel,
                contentDescription = stringResource(R.string.home_description_img_publication_user),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .onSizeChanged { containerSize = it }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .transformable(state = transformableState)
                    .background(Color.Black),
                contentScale = ContentScale.Fit,
                loading = {
                    ShimmerPlaceholder(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                    )
                },
                error = {
                    Image(
                        painter = imagePlaceholder,
                        contentDescription = stringResource(R.string.home_description_img_publication_user),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentScale = ContentScale.Fit,
                    )
                },
            )

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_image_viewer_close),
                    color = Color.White,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = stringResource(R.string.home_image_viewer_hint),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun clampOffset(
    offset: Offset,
    scale: Float,
    containerSize: IntSize,
): Offset {
    if (containerSize == IntSize.Zero || scale <= 1f) return Offset.Zero

    val maxX = ((containerSize.width * (scale - 1f)) / 2f).coerceAtLeast(0f)
    val maxY = ((containerSize.height * (scale - 1f)) / 2f).coerceAtLeast(0f)

    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY),
    )
}

internal data class ExpandedImageState(
    val imageModel: Any,
    val title: String,
)
