package com.pdf.alldocument.office.pdfreaderapp.presentation.tools

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdf.alldocument.office.pdfreaderapp.R
import kotlinx.coroutines.launch

// Tactile spring press animation
fun Modifier.tactileClick(onClick: () -> Unit): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tactileScale"
    )
    this
        .scale(scale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = { onClick() }
            )
        }
}

/**
 * Custom scrollbar drawn directly on content, giving immediate visual feedback
 * that the screen can be scrolled and displaying current scroll position.
 */
fun Modifier.drawVerticalScrollbar(
    state: ScrollState,
    color: Color = Color(0xFF94A3B8).copy(alpha = 0.55f),
    width: Dp = 4.dp
): Modifier = drawWithContent {
    drawContent()

    val totalHeight = state.maxValue + size.height
    if (totalHeight > size.height && state.maxValue > 0) {
        val scrollbarHeight = (size.height * (size.height / totalHeight)).coerceIn(36.dp.toPx(), size.height / 2)
        val scrollbarY = (state.value.toFloat() / state.maxValue.toFloat()) * (size.height - scrollbarHeight)

        drawRoundRect(
            color = color,
            topLeft = Offset(size.width - width.toPx() - 2.dp.toPx(), scrollbarY),
            size = Size(width.toPx(), scrollbarHeight),
            cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2)
        )
    }
}

/**
 * Horizontal quick jump category pills so users immediately see all sections
 * available on the tools screen and can tap any pill to jump right to it.
 */
@Composable
private fun ToolsCategoryTabs(
    activeCategory: String,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = remember {
        listOf(
            "All" to "✨ All",
            "PDF Tools" to "📄 PDF Tools",
            "Sign" to "✍️ Sign PDF",
            "Convert" to "🔄 Convert",
            "Scan" to "📷 Scan",
            "AI" to "🤖 AI Reading"
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { (key, label) ->
            val isSelected = activeCategory == key
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onCategoryClick(key) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0))
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else Color(0xFF475569),
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp)
                )
            }
        }
    }
}

private data class ConvertItem(
    val title: String,
    val subtitle: String,
    @DrawableRes val iconRes: Int,
    val onClick: () -> Unit
)

private data class GridToolItem(
    val title: String,
    @DrawableRes val iconRes: Int,
    val tintColor: Color = Color.Transparent,
    val badge: String? = null,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsHomeScreen(
    onNavigateToMerge: () -> Unit,
    onNavigateToSign: (String, String) -> Unit,
    onNavigateToOrganize: (String, String) -> Unit,
    onNavigateToTextToPdf: () -> Unit = {},
    onNavigateToPdfToImage: () -> Unit = {},
    onNavigateToPdfToLongImage: () -> Unit = {},
    onNavigateToLockPdf: () -> Unit = {},
    onNavigateToUnlockPdf: () -> Unit = {},
    onNavigateToSplitPdf: () -> Unit = {},
    onNavigateToImageToPdf: () -> Unit = {},
    onNavigateToScanToPdf: () -> Unit = {},
    onNavigateToImageToText: () -> Unit = {},
    onNavigateToIdCardScanner: () -> Unit = {},
    onNavigateToIdPhotoMaker: () -> Unit = {},
    onNavigateToAiReading: () -> Unit = {},
    onNavigateToScanToExcel: () -> Unit = {},
    onNavigateToOfficeToPdf: () -> Unit = {},
    onNavigateToWordToPdf: () -> Unit = {},
    onNavigateToPdfToWord: () -> Unit = {},
    onNavigateToCompress: () -> Unit = {},
    onNavigateToEditPdf: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val context = LocalContext.current

    val onSendFeedback: () -> Unit = remember(context) {
        {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@example.com")
                putExtra(Intent.EXTRA_SUBJECT, "App Feedback / Feature Request")
            }
            try {
                context.startActivity(Intent.createChooser(intent, "Send Feedback"))
            } catch (_: Exception) {
            }
        }
    }

    // Convert Studio Tools (10 items - 2 columns)
    val convertItems = remember {
        listOf(
            ConvertItem("Image to PDF", "Fast Convert", R.drawable.imagetopdf, onNavigateToImageToPdf),
            ConvertItem("Scan to PDF", "Instant Scan", R.drawable.scantopdf, onNavigateToScanToPdf),
            ConvertItem("Word to PDF", "Doc to PDF", R.drawable.wordtopdf, onNavigateToWordToPdf),
            ConvertItem("PDF to Word", "Easy Convert", R.drawable.pdftoword, onNavigateToPdfToWord),
            ConvertItem("PDF to Image", "Smart Capture", R.drawable.pdftoimage, onNavigateToPdfToImage),
            ConvertItem("Image to Text", "Text Capture", R.drawable.imagetotext, onNavigateToImageToText),
            ConvertItem("Excel to PDF", "Grid Convert", R.drawable.exceltopdf, onNavigateToOfficeToPdf),
            ConvertItem("PPT to PDF", "Smart Export", R.drawable.ppttopdf, onNavigateToOfficeToPdf),
            ConvertItem("Text to Pdf", "Quick Capture", R.drawable.texttopdf, onNavigateToTextToPdf),
            ConvertItem("PDF to Long", "Image", R.drawable.pdftolongimage, onNavigateToPdfToLongImage)
        )
    }

    // PDF Tools (Icons without background cards)
    val pdfTools = remember {
        listOf(
            GridToolItem("Sign PDF", R.drawable.signpdf, badge = "NEW", onClick = { onNavigateToSign("", "Sign PDF") }),
            GridToolItem("Merge PDF", R.drawable.mergepdf, onClick = onNavigateToMerge),
            GridToolItem("Split PDF", R.drawable.splitpdf, onClick = onNavigateToSplitPdf),
            GridToolItem("Edit PDF", R.drawable.editpdf, onClick = onNavigateToEditPdf),
            GridToolItem("Compress PDF", R.drawable.compresspdf, onClick = onNavigateToCompress),
            GridToolItem("Lock PDF", R.drawable.lockpdf, onClick = onNavigateToLockPdf),
            GridToolItem("Unlock PDF", R.drawable.unlockpdf, onClick = onNavigateToUnlockPdf),
            GridToolItem("Change PDF\nPages Order", R.drawable.organizepdf, onClick = { onNavigateToOrganize("", "Change PDF Pages Order") })
        )
    }

    // Scan Tools (Circular soft tinted backgrounds)
    val scanTools = remember {
        listOf(
            GridToolItem("ID Card", R.drawable.idcard, tintColor = Color(0xFFE0F7FA), onClick = onNavigateToIdCardScanner),
            GridToolItem("Scan to PDF", R.drawable.scantopdf, tintColor = Color(0xFFFEE2E2), onClick = onNavigateToScanToPdf),
            GridToolItem("ID Photo Maker", R.drawable.idphoto, tintColor = Color(0xFFE0F2FE), onClick = onNavigateToIdPhotoMaker),
            GridToolItem("Scan to Excel", R.drawable.scantoexcel, tintColor = Color(0xFFE8F5E9), onClick = onNavigateToScanToExcel)
        )
    }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var aiY by remember { mutableFloatStateOf(0f) }
    var convertY by remember { mutableFloatStateOf(0f) }
    var pdfToolsY by remember { mutableFloatStateOf(0f) }
    var scanY by remember { mutableFloatStateOf(0f) }

    val activeCategory = remember(scrollState.value, convertY, pdfToolsY, scanY) {
        val currentScroll = scrollState.value + 120
        when {
            scanY > 0 && currentScroll >= scanY -> "Scan"
            pdfToolsY > 0 && currentScroll >= pdfToolsY -> "PDF Tools"
            convertY > 0 && currentScroll >= convertY -> "Convert"
            aiY > 0 && currentScroll >= aiY -> "AI"
            else -> "All"
        }
    }

    val onCategoryClick: (String) -> Unit = { category ->
        when (category) {
            "Sign" -> onNavigateToSign("", "Sign PDF")
            "All" -> coroutineScope.launch { scrollState.animateScrollTo(0) }
            "PDF Tools" -> coroutineScope.launch { scrollState.animateScrollTo((pdfToolsY.toInt() - 10).coerceAtLeast(0)) }
            "Convert" -> coroutineScope.launch { scrollState.animateScrollTo((convertY.toInt() - 10).coerceAtLeast(0)) }
            "Scan" -> coroutineScope.launch { scrollState.animateScrollTo((scanY.toInt() - 10).coerceAtLeast(0)) }
            "AI" -> coroutineScope.launch { scrollState.animateScrollTo(aiY.toInt().coerceAtLeast(0)) }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PDF Tools",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF0F172A)
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFE8F5E9),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                            modifier = Modifier.tactileClick { onNavigateToSign("", "Sign PDF") }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.signpdf),
                                    contentDescription = "Sign PDF",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Sign PDF",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }
                    }

                    // Horizontal quick-jump category pills
                    ToolsCategoryTabs(
                        activeCategory = activeCategory,
                        onCategoryClick = onCategoryClick
                    )

                    HorizontalDivider(
                        color = Color(0xFFF1F5F9),
                        thickness = 1.dp
                    )
                }
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .drawVerticalScrollbar(scrollState)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // ================= 1. AI FEATURES =================
                Text(
                    text = "AI Features",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A),
                    modifier = Modifier
                        .onGloballyPositioned { aiY = it.positionInParent().y }
                        .padding(bottom = 10.dp)
                )

                // Warm Orange Gradient Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFE082), // Warm soft gold
                                    Color(0xFFFFB74D), // Golden orange
                                    Color(0xFFFFA726)  // Vibrant orange
                                )
                            )
                        )
                        .tactileClick(onClick = onNavigateToAiReading)
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    // Subtle AI Watermark
                    Text(
                        text = "AI",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.28f),
                        modifier = Modifier.align(Alignment.Center)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "AI Reading",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF451A03)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Clarifying Your Content",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF78350F).copy(alpha = 0.85f)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            shadowElevation = 1.dp,
                            modifier = Modifier.tactileClick(onClick = onNavigateToAiReading)
                        ) {
                            Text(
                                text = "Use",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color(0xFF451A03),
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ================= 2. CONVERT =================
                Text(
                    text = "Convert",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A),
                    modifier = Modifier
                        .onGloballyPositioned { convertY = it.positionInParent().y }
                        .padding(bottom = 10.dp)
                )

                // 2-Column Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    convertItems.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { item ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .tactileClick(onClick = item.onClick),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                    shadowElevation = 0.5.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = painterResource(id = item.iconRes),
                                            contentDescription = item.title,
                                            modifier = Modifier.size(34.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.5.sp,
                                                color = Color(0xFF0F172A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.subtitle,
                                                fontSize = 10.5.sp,
                                                color = Color(0xFF94A3B8),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ================= 3. PDF TOOLS =================
                Text(
                    text = "PDF Tools",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A),
                    modifier = Modifier
                        .onGloballyPositioned { pdfToolsY = it.positionInParent().y }
                        .padding(bottom = 12.dp)
                )

                // 4 Items Per Row - NO CARDS!
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    pdfTools.chunked(4).forEach { rowTools ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            rowTools.forEach { tool ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .tactileClick(onClick = tool.onClick),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.TopEnd) {
                                            Image(
                                                painter = painterResource(id = tool.iconRes),
                                                contentDescription = tool.title,
                                                modifier = Modifier.size(42.dp)
                                            )
                                            if (tool.badge != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .offset(x = 6.dp, y = (-4).dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFF10B981))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = tool.badge,
                                                        color = Color.White,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = tool.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1E293B),
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }

                            // Fill empty cells if row has fewer than 4 items
                            repeat(4 - rowTools.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ================= 4. SCAN =================
                Text(
                    text = "Scan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A),
                    modifier = Modifier
                        .onGloballyPositioned { scanY = it.positionInParent().y }
                        .padding(bottom = 12.dp)
                )

                // 4 Items in a row with circular soft tinted background - NO CARDS!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    scanTools.forEach { tool ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .tactileClick(onClick = tool.onClick),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(tool.tintColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = tool.iconRes),
                                        contentDescription = tool.title,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = tool.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ================= 5. FEEDBACK BANNER =================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tactileClick(onClick = onSendFeedback),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Looking for something else?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                border = BorderStroke(1.2.dp, Color(0xFFEF4444)),
                                modifier = Modifier.tactileClick(onClick = onSendFeedback)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = "👍", fontSize = 12.sp)
                                    Text(
                                        text = "Tell us",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.Outlined.RateReview,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(72.dp))
            }

            // Floating "Scroll for more tools" affordance (visible at top when scrollable)
            AnimatedVisibility(
                visible = scrollState.value < 40 && scrollState.canScrollForward,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "bounce")
                val bounceOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 6f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bounce"
                )

                Surface(
                    onClick = {
                        coroutineScope.launch {
                            val target = if (pdfToolsY > 0) pdfToolsY.toInt() - 10 else scrollState.value + 350
                            scrollState.animateScrollTo(target)
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0F172A),
                    shadowElevation = 6.dp,
                    modifier = Modifier.offset(y = bounceOffset.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Scroll for more tools",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll down",
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Floating "Back to top" button
            AnimatedVisibility(
                visible = scrollState.value > 350,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Surface(
                    onClick = {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(0)
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.92f),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Back to top",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Back to top",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
