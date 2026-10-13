package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.idcardscanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Dimmed overlay with a transparent ID-card sized cutout (ratio 85.6 x 54 mm = ~1.586)
 * that guides the user to align the card.
 */
@Composable
fun IdCardOverlay(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        drawRect(Color.Black.copy(alpha = 0.6f))
        val w = size.width * 0.9f
        val h = w / 1.586f
        val topLeft = Offset((size.width - w) / 2f, (size.height - h) / 2f)
        drawRoundRect(
            color = Color.Transparent,
            topLeft = topLeft,
            size = Size(w, h),
            cornerRadius = CornerRadius(24f),
            blendMode = BlendMode.Clear
        )
        drawRoundRect(
            color = Color.White,
            topLeft = topLeft,
            size = Size(w, h),
            cornerRadius = CornerRadius(24f),
            style = Stroke(width = 4f)
        )
    }
}
