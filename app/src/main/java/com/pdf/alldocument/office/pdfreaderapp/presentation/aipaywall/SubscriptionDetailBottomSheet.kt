package com.pdf.alldocument.office.pdfreaderapp.presentation.aipaywall

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionDetailBottomSheet(
    onDismissRequest: () -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val bulletPoints = listOf(
        "By subscribing to the PDF Reader service, you'll enjoy an ad-free experience.",
        "After the purchase is verified, the subscription will be billed via the Google Play Store.",
        "After cancellation, the subscription will remain active unite the end of the annual billing period.",
        "Subscription will be automatically renewed unless you disable auto-renewal within 24 hours before the current period expires.",
        "You have the option to control your subscription or disable automatic renewal within your Google Play Store account settings.",
        "A subscription canceled during the trial period with incur no changes."
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = modalBottomSheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 10.dp),
                color = Color(0xFFE2E8F0),
                shape = RoundedCornerShape(2.dp)
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Subscription Detail",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(bottom = 20.dp, top = 4.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                bulletPoints.forEach { pointText ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Green stylized arrow icon matching 137.png
                        GreenDoubleArrowIcon(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = pointText,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981) // Green matching 137.png
                )
            ) {
                Text(
                    text = "Close",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Green double-arrowhead bullet icon matching screenshot 137.png
 */
@Composable
private fun GreenDoubleArrowIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path1 = Path().apply {
            moveTo(w * 0.15f, h * 0.2f)
            lineTo(w * 0.5f, h * 0.5f)
            lineTo(w * 0.15f, h * 0.8f)
        }
        val path2 = Path().apply {
            moveTo(w * 0.45f, h * 0.2f)
            lineTo(w * 0.8f, h * 0.5f)
            lineTo(w * 0.45f, h * 0.8f)
        }

        val stroke = Stroke(width = 2.dp.toPx())
        val arrowColor = Color(0xFF10B981)

        drawPath(path = path1, color = arrowColor, style = stroke)
        drawPath(path = path2, color = arrowColor, style = stroke)
    }
}
