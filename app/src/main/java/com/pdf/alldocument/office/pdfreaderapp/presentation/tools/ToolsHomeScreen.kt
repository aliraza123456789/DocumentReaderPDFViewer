package com.pdf.alldocument.office.pdfreaderapp.presentation.tools

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.CallMerge
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

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

private data class ToolSearchItem(
    val title: String,
    val subtitle: String,
    val category: String,
    val icon: ImageVector,
    val gradient: List<Color>,
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
    onNavigateToPdfToWord: () -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    // Master searchable tools list
    val allTools = remember {
        listOf(
            ToolSearchItem("Image to PDF", "Fast photo to PDF converter", "Convert", Icons.Outlined.Image, listOf(Color(0xFF10B981), Color(0xFF059669)), "FAST", onNavigateToImageToPdf),
            ToolSearchItem("Scan to PDF", "Instant camera document scan", "Camera & Scan", Icons.Outlined.DocumentScanner, listOf(Color(0xFF06B6D4), Color(0xFF0284C7)), "HOT", onNavigateToScanToPdf),
            ToolSearchItem("Word to PDF", "Convert Word DOCX to PDF", "Convert", Icons.Outlined.Description, listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)), null, onNavigateToWordToPdf),
            ToolSearchItem("PDF to Word", "Export PDF to editable Word", "Convert", Icons.Outlined.Description, listOf(Color(0xFFEF4444), Color(0xFF2563EB)), "POPULAR", onNavigateToPdfToWord),
            ToolSearchItem("Excel to PDF", "Convert spreadsheets to PDF", "Convert", Icons.Outlined.TableChart, listOf(Color(0xFF059669), Color(0xFF047857)), null, onNavigateToOfficeToPdf),
            ToolSearchItem("PPT to PDF", "Convert PowerPoint slides to PDF", "Convert", Icons.Outlined.Slideshow, listOf(Color(0xFFEA580C), Color(0xFFC2410C)), null, onNavigateToOfficeToPdf),
            ToolSearchItem("PDF to Image", "Extract high quality JPG/PNG", "Convert", Icons.Outlined.Image, listOf(Color(0xFFEF4444), Color(0xFF10B981)), null, onNavigateToPdfToImage),
            ToolSearchItem("Text to PDF", "Create PDF from plain text", "Convert", Icons.Outlined.TextFields, listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)), null, onNavigateToTextToPdf),
            ToolSearchItem("PDF to Long Image", "Stitch pages into continuous image", "Convert", Icons.Outlined.ViewAgenda, listOf(Color(0xFFEF4444), Color(0xFF06B6D4)), null, onNavigateToPdfToLongImage),
            ToolSearchItem("Merge PDF", "Combine multiple PDFs into one", "PDF Tools", Icons.AutoMirrored.Outlined.CallMerge, listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)), "ESSENTIAL", onNavigateToMerge),
            ToolSearchItem("Split PDF", "Extract pages from PDF", "PDF Tools", Icons.AutoMirrored.Outlined.CallSplit, listOf(Color(0xFFF43F5E), Color(0xFFE11D48)), null, onNavigateToSplitPdf),
            ToolSearchItem("Sign PDF", "Draw digital signature on PDF", "PDF Tools", Icons.Outlined.Draw, listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)), "NEW", { onNavigateToSign("", "Sign PDF") }),
            ToolSearchItem("Lock PDF", "Encrypt & set password", "PDF Tools", Icons.Outlined.Lock, listOf(Color(0xFF4B5563), Color(0xFF1F2937)), null, onNavigateToLockPdf),
            ToolSearchItem("Unlock PDF", "Remove PDF password", "PDF Tools", Icons.Outlined.LockOpen, listOf(Color(0xFF10B981), Color(0xFF059669)), null, onNavigateToUnlockPdf),
            ToolSearchItem("Organize PDF", "Reorder, rotate & delete pages", "PDF Tools", Icons.Outlined.GridView, listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)), null, { onNavigateToOrganize("", "Organize PDF") }),
            ToolSearchItem("ID Card Scanner", "Scan 2-sided identity card to A4", "Camera & Scan", Icons.Outlined.Badge, listOf(Color(0xFF06B6D4), Color(0xFF0E7490)), "SMART", onNavigateToIdCardScanner),
            ToolSearchItem("Extract Text (OCR)", "Recognize text from camera/image", "Camera & Scan", Icons.Outlined.TextFields, listOf(Color(0xFFA855F7), Color(0xFF7E22CE)), "OCR", onNavigateToImageToText),
            ToolSearchItem("ID Photo Maker", "Create passport sized photos", "Camera & Scan", Icons.Outlined.AccountBox, listOf(Color(0xFF38BDF8), Color(0xFF0284C7)), null, onNavigateToIdPhotoMaker),
            ToolSearchItem("Scan to Excel", "Extract tables directly to Excel", "Camera & Scan", Icons.Outlined.QrCodeScanner, listOf(Color(0xFF16A34A), Color(0xFF15803D)), "AI", onNavigateToScanToExcel)
        )
    }

    val searchResults = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else allTools.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.subtitle.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    val onSendFeedback: () -> Unit = remember(context) {
        {
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:support@example.com")
                putExtra(android.content.Intent.EXTRA_SUBJECT, "App Feedback / Feature Request")
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Send Feedback"))
        }
    }

    Scaffold(
        topBar = {
            ToolsHeaderTopBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onFeedbackClick = onSendFeedback
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            if (searchQuery.isNotBlank()) {
                // Search Results State
                SearchResultsSection(results = searchResults, onClear = { searchQuery = "" })
            } else {
                // 1. (Removed AI Hero Banner)

                Spacer(modifier = Modifier.height(20.dp))

                // 2. SMART SCAN & OCR HUB (Horizontal Pods)
                SmartScanSection(
                    onScanToPdf = onNavigateToScanToPdf,
                    onIdCard = onNavigateToIdCardScanner,
                    onExtractText = onNavigateToImageToText,
                    onIdPhoto = onNavigateToIdPhotoMaker,
                    onScanToExcel = onNavigateToScanToExcel
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 3. CORE PDF TOOLS (High-Visibility 3-Column Squircles)
                CorePdfToolsSection(
                    onMerge = onNavigateToMerge,
                    onSplit = onNavigateToSplitPdf,
                    onSign = { onNavigateToSign("", "Sign PDF") },
                    onLock = onNavigateToLockPdf,
                    onUnlock = onNavigateToUnlockPdf,
                    onOrganize = { onNavigateToOrganize("", "Organize PDF") }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 4. CONVERT STUDIO (2-Column Asymmetric Cards)
                ConvertStudioSection(
                    onImageToPdf = onNavigateToImageToPdf,
                    onWordToPdf = onNavigateToWordToPdf,
                    onPdfToWord = onNavigateToPdfToWord,
                    onExcelToPdf = onNavigateToOfficeToPdf,
                    onPptToPdf = onNavigateToOfficeToPdf,
                    onPdfToImage = onNavigateToPdfToImage,
                    onTextToPdf = onNavigateToTextToPdf,
                    onPdfToLongImage = onNavigateToPdfToLongImage
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 5. FEATURE REQUEST & FEEDBACK BANNER
                FeedbackInteractiveCard(onClick = onSendFeedback)

                Spacer(modifier = Modifier.height(16.dp))

                // 6. 100% PRIVATE & OFFLINE TRUST PILL
                OfflinePrivacyFooter()

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }


}

/* =====================================================================
 * TOP APP BAR WITH SEARCH BAR
 * ===================================================================== */
@Composable
private fun ToolsHeaderTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFeedbackClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF7C3AED), Color(0xFF3B82F6))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PDF & Document Tools",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "All-in-one offline power utilities",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Suggest a feature action button
                IconButton(
                    onClick = onFeedbackClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = "Suggest Feature",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Search Pill
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search any tool (e.g. merge, word, sign)...",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color(0xFF7C3AED)
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/* =====================================================================
 * SEARCH RESULTS STATE
 * ===================================================================== */
@Composable
private fun SearchResultsSection(
    results: List<ToolSearchItem>,
    onClear: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Results (${results.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF1E293B)
            )
            TextButton(onClick = onClear) {
                Text("Clear Search", color = Color(0xFF7C3AED), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tools matched your search query.",
                    color = Color(0xFF64748B),
                    fontSize = 14.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                results.forEach { item ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tactileClick(onClick = item.onClick),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(item.gradient)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    if (item.badge != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFEDE9FE))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = item.badge,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF7C3AED)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = item.subtitle,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/* =====================================================================
 * 1. HERO AI READING BANNER
 * ===================================================================== */
@Composable
private fun AiHeroGlassBanner(onLaunchClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .tactileClick(onClick = onLaunchClick),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6D28D9),
                            Color(0xFF7C3AED),
                            Color(0xFF3B82F6)
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Icon + Text
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = "AI Assistant",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AI Document Assistant",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFDE047))
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = "NEW",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF78350F)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Summarize, extract & chat with PDFs",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }
                }

                // Launch CTA Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Try",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF6D28D9)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF6D28D9),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/* =====================================================================
 * 2. SMART SCAN & CAPTURE SECTION (HORIZONTAL SNAP PODS)
 * ===================================================================== */
private data class ScanPodItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val badge: String?,
    val onClick: () -> Unit
)

@Composable
private fun SmartScanSection(
    onScanToPdf: () -> Unit,
    onIdCard: () -> Unit,
    onExtractText: () -> Unit,
    onIdPhoto: () -> Unit,
    onScanToExcel: () -> Unit
) {
    val pods = remember {
        listOf(
            ScanPodItem("Scan to PDF", "Instant Cam", Icons.Outlined.DocumentScanner, listOf(Color(0xFF06B6D4), Color(0xFF0284C7)), "POPULAR", onScanToPdf),
            ScanPodItem("ID Card", "Both Sides", Icons.Outlined.Badge, listOf(Color(0xFF0EA5E9), Color(0xFF2563EB)), "SMART", onIdCard),
            ScanPodItem("Extract Text", "OCR to Text", Icons.Outlined.TextFields, listOf(Color(0xFFA855F7), Color(0xFF7C3AED)), "AI", onExtractText),
            ScanPodItem("ID Photo", "Passport Size", Icons.Outlined.AccountBox, listOf(Color(0xFF38BDF8), Color(0xFF0284C7)), null, onIdPhoto),
            ScanPodItem("Scan to Excel", "Table Scanner", Icons.Outlined.QrCodeScanner, listOf(Color(0xFF10B981), Color(0xFF059669)), "XLSX", onScanToExcel)
        )
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "📷 Smart Scan & Camera",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                )
            }
            Text(
                text = "Swipe →",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(pods) { pod ->
                Surface(
                    modifier = Modifier
                        .width(108.dp)
                        .tactileClick(onClick = pod.onClick),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(pod.gradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = pod.icon,
                                contentDescription = pod.title,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = pod.title,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = pod.subtitle,
                            fontSize = 9.5.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/* =====================================================================
 * 3. CORE PDF TOOLS (HIGH VISIBILITY 3-COLUMN SQUIRCLES)
 * ===================================================================== */
private data class CorePdfTool(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val onClick: () -> Unit
)

@Composable
private fun CorePdfToolsSection(
    onMerge: () -> Unit,
    onSplit: () -> Unit,
    onSign: () -> Unit,
    onLock: () -> Unit,
    onUnlock: () -> Unit,
    onOrganize: () -> Unit
) {
    val tools = remember {
        listOf(
            CorePdfTool("Merge PDF", "Combine files", Icons.AutoMirrored.Outlined.CallMerge, listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)), onMerge),
            CorePdfTool("Split PDF", "Extract pages", Icons.AutoMirrored.Outlined.CallSplit, listOf(Color(0xFFF43F5E), Color(0xFFBE123C)), onSplit),
            CorePdfTool("Sign PDF", "Digital ink", Icons.Outlined.Draw, listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)), onSign),
            CorePdfTool("Lock PDF", "Add password", Icons.Outlined.Lock, listOf(Color(0xFF475569), Color(0xFF1E293B)), onLock),
            CorePdfTool("Unlock PDF", "Remove code", Icons.Outlined.LockOpen, listOf(Color(0xFF059669), Color(0xFF047857)), onUnlock),
            CorePdfTool("Organize", "Reorder pages", Icons.Outlined.GridView, listOf(Color(0xFF7C3AED), Color(0xFF5B21B6)), onOrganize)
        )
    }

    Column {
        Text(
            text = "🛡️ PDF Security & Edit",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color(0xFF0F172A),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tools.chunked(3).forEach { rowTools ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowTools.forEach { tool ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .tactileClick(onClick = tool.onClick),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.linearGradient(tool.gradient)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tool.icon,
                                        contentDescription = tool.title,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(7.dp))

                                Text(
                                    text = tool.title,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = tool.subtitle,
                                    fontSize = 9.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* =====================================================================
 * 4. CONVERT STUDIO (2-COLUMN DUAL-TYPE CARDS)
 * ===================================================================== */
private data class ConvertCardItem(
    val title: String,
    val subtitle: String,
    val fromBadge: String,
    val toBadge: String,
    val fromColor: Color,
    val toColor: Color,
    val onClick: () -> Unit
)

@Composable
private fun ConvertStudioSection(
    onImageToPdf: () -> Unit,
    onWordToPdf: () -> Unit,
    onPdfToWord: () -> Unit,
    onExcelToPdf: () -> Unit,
    onPptToPdf: () -> Unit,
    onPdfToImage: () -> Unit,
    onTextToPdf: () -> Unit,
    onPdfToLongImage: () -> Unit
) {
    val items = remember {
        listOf(
            ConvertCardItem("Image to PDF", "Photos to doc", "IMG", "PDF", Color(0xFF10B981), Color(0xFFEF4444), onImageToPdf),
            ConvertCardItem("PDF to Word", "Editable Docx", "PDF", "DOC", Color(0xFFEF4444), Color(0xFF2563EB), onPdfToWord),
            ConvertCardItem("Word to PDF", "Preserve layout", "DOC", "PDF", Color(0xFF2563EB), Color(0xFFEF4444), onWordToPdf),
            ConvertCardItem("Excel to PDF", "Grid & sheets", "XLS", "PDF", Color(0xFF059669), Color(0xFFEF4444), onExcelToPdf),
            ConvertCardItem("PPT to PDF", "Slide export", "PPT", "PDF", Color(0xFFEA580C), Color(0xFFEF4444), onPptToPdf),
            ConvertCardItem("PDF to Image", "JPG & PNG extraction", "PDF", "IMG", Color(0xFFEF4444), Color(0xFF10B981), onPdfToImage),
            ConvertCardItem("Text to PDF", "Note to document", "TXT", "PDF", Color(0xFF7C3AED), Color(0xFFEF4444), onTextToPdf),
            ConvertCardItem("Long Image", "Stitch PDF pages", "PDF", "LONG", Color(0xFFEF4444), Color(0xFF06B6D4), onPdfToLongImage)
        )
    }

    Column {
        Text(
            text = "🔄 Convert Studio",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color(0xFF0F172A),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .tactileClick(onClick = item.onClick),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic Two-Badge Visual: e.g. [IMG] ➔ [PDF]
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(item.fromColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.fromBadge.take(3),
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier
                                            .size(10.dp)
                                            .padding(horizontal = 1.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(item.toColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.toBadge.take(3),
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.subtitle,
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* =====================================================================
 * 5. FEEDBACK / SUGGEST A FEATURE INTERACTIVE CARD
 * ===================================================================== */
@Composable
private fun FeedbackInteractiveCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .tactileClick(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFEF3C7).copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Color(0xFFFDE68A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Need another PDF tool?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color(0xFF92400E)
                    )
                    Text(
                        text = "Tell us what feature to add next",
                        fontSize = 11.sp,
                        color = Color(0xFFB45309)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF59E0B))
            ) {
                Text(
                    text = "Suggest 💡",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = Color(0xFFB45309),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/* =====================================================================
 * 6. TRUST & OFFLINE PRIVACY FOOTER
 * ===================================================================== */
@Composable
private fun OfflinePrivacyFooter() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF0FDF4),
        border = BorderStroke(1.dp, Color(0xFFDCFCE7))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = "Security",
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "100% Private & Device Local",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF15803D)
                )
                Text(
                    text = "Your files never leave this device. Zero cloud uploads.",
                    fontSize = 10.sp,
                    color = Color(0xFF16A34A)
                )
            }
        }
    }
}

