package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.aireading

import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.BrandPurple
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.BrandPurpleDeep
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.BrandPurpleEnd

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiReadingScreen(
    onBack: () -> Unit,
    viewModel: AiReadingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val name = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst() && i >= 0) c.getString(i) else null
        } ?: "document.pdf"
        viewModel.loadDocument(uri, name)
    }

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.text) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // AI Gradient Badge Icon
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(BrandPurpleDeep, BrandPurpleEnd)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "AI Copilot",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFEDE9FE)
                                        ) {
                                            Text(
                                                text = "PRO",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = BrandPurpleDeep,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Online pulse indicator
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFF10B981), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Ultra-Fast Groq Engine",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF334155)
                                )
                            }
                        },
                        actions = {
                            if (state.messages.isNotEmpty()) {
                                IconButton(onClick = viewModel::clearChat) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Clear Chat",
                                        tint = Color(0xFF64748B)
                                    )
                                }
                            }
                            IconButton(onClick = { picker.launch(arrayOf("application/pdf")) }) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Attach PDF",
                                    tint = if (state.documentName != null) BrandPurpleDeep else Color(0xFF64748B)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )

                    // Attached Document Banner
                    AnimatedVisibility(visible = state.documentName != null) {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEE2E2),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = state.documentName ?: "",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${state.documentChars} characters loaded • Active in memory",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                IconButton(
                                    onClick = viewModel::detachDocument,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Detach",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (state.isExtracting) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = BrandPurpleDeep,
                            trackColor = Color(0xFFEDE9FE)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    // Quick Action Chips Row
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickAction.entries.forEach { a ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (state.documentName != null) Color(0xFFF5F3FF) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    1.dp,
                                    if (state.documentName != null) Color(0xFFDDD6FE) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(enabled = !state.isResponding && !state.isExtracting) {
                                        if (state.documentName != null) {
                                            viewModel.runQuickAction(a)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Please attach a PDF first to use ${a.label}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            picker.launch(arrayOf("application/pdf"))
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (a) {
                                            QuickAction.SUMMARIZE -> Icons.Default.ElectricBolt
                                            QuickAction.STUDY_NOTES -> Icons.Default.MenuBook
                                            QuickAction.QA -> Icons.Default.Quiz
                                            QuickAction.KEY_TAKEAWAYS -> Icons.Default.FormatListNumbered
                                            QuickAction.EXPLAIN_TERMS -> Icons.Default.Lightbulb
                                        },
                                        contentDescription = null,
                                        tint = if (state.documentName != null) BrandPurpleDeep else Color(0xFF64748B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = a.label,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (state.documentName != null) BrandPurpleDeep else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }

                    // Error Banner
                    AnimatedVisibility(visible = state.error != null) {
                        state.error?.let { err ->
                            Surface(
                                color = Color(0xFFFEF2F2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFFECDD3))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFE11D48),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = err,
                                        color = Color(0xFF9F1239),
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = viewModel::clearError,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = Color(0xFF9F1239),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Modern Floating Input Dock
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // PDF Attach Icon Button
                        Surface(
                            shape = CircleShape,
                            color = if (state.documentName != null) Color(0xFFEDE9FE) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { picker.launch(arrayOf("application/pdf")) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Attach Document",
                                    tint = if (state.documentName != null) BrandPurpleDeep else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Text Field Container
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it },
                                placeholder = {
                                    Text(
                                        text = if (state.documentName != null) "Ask about ${state.documentName}..." else "Ask AI anything or attach a PDF...",
                                        fontSize = 13.5.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Send / Stop Glowing Button
                        if (state.isResponding) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEF4444),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = viewModel::stop)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        } else {
                            val canSend = input.isNotBlank() && !state.isExtracting
                            Surface(
                                shape = CircleShape,
                                shadowElevation = if (canSend) 4.dp else 0.dp,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (canSend) {
                                            Brush.linearGradient(listOf(BrandPurpleDeep, BrandPurpleEnd))
                                        } else {
                                            Brush.linearGradient(listOf(Color(0xFFCBD5E1), Color(0xFFCBD5E1)))
                                        }
                                    )
                                    .clickable(enabled = canSend) {
                                        viewModel.send(input)
                                        input = ""
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (canSend) {
                                                Brush.linearGradient(listOf(BrandPurpleDeep, BrandPurpleEnd))
                                            } else {
                                                Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0)))
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = if (canSend) Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            if (state.messages.isEmpty()) {
                // Empty State: Futuristic AI Copilot Hero Hub
                AiHeroWelcomeHub(
                    documentName = state.documentName,
                    documentChars = state.documentChars,
                    onAttachPdf = { picker.launch(arrayOf("application/pdf")) },
                    onActionClick = { action ->
                        if (state.documentName != null) {
                            viewModel.runQuickAction(action)
                        } else {
                            Toast.makeText(
                                context,
                                "Attach a PDF to run ${action.label}",
                                Toast.LENGTH_SHORT
                            ).show()
                            picker.launch(arrayOf("application/pdf"))
                        }
                    }
                )
            } else {
                // Chat Message Stream
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        if (message.fromUser) {
                            UserMessageBubble(text = message.text)
                        } else {
                            AiResponseBubble(
                                text = message.text,
                                isStreaming = state.isResponding && message.text.isEmpty(),
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(message.text))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Futuristic Hero Welcome Hub shown when chat is fresh.
 */
@Composable
private fun AiHeroWelcomeHub(
    documentName: String?,
    documentChars: Int,
    onAttachPdf: () -> Unit,
    onActionClick: (QuickAction) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing Futuristic AI Orb
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BrandPurpleEnd.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(BrandPurpleDeep, BrandPurpleEnd)
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(BrandPurpleDeep, BrandPurpleEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "AI Document Intelligence",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Chat, summarize, extract key formulas, generate quizzes, and analyze any document in seconds.",
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // PDF Attachment Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onAttachPdf),
            shape = RoundedCornerShape(16.dp),
            color = if (documentName != null) Color(0xFFF0FDF4) else Color.White,
            border = BorderStroke(
                width = 1.5.dp,
                color = if (documentName != null) Color(0xFF86EFAC) else Color(0xFFDDD6FE)
            ),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (documentName != null) Color(0xFFDCFCE7) else Color(0xFFEDE9FE),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (documentName != null) Icons.Default.CheckCircle else Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = if (documentName != null) Color(0xFF16A34A) else BrandPurpleDeep,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = documentName ?: "Tap to Attach a PDF Document",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (documentName != null) {
                            "$documentChars characters loaded • Ready for questions"
                        } else {
                            "Unlocks instant summary, quiz generation & deep Q&A"
                        },
                        fontSize = 11.5.sp,
                        color = if (documentName != null) Color(0xFF16A34A) else Color(0xFF64748B)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4 Action Preset Cards (2x2 Grid)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionCard(
                    title = "⚡ Summary",
                    subtitle = "Concise key highlights",
                    modifier = Modifier.weight(1f),
                    onClick = { onActionClick(QuickAction.SUMMARIZE) }
                )
                ActionCard(
                    title = "📝 Study Notes",
                    subtitle = "Definitions & formulas",
                    modifier = Modifier.weight(1f),
                    onClick = { onActionClick(QuickAction.STUDY_NOTES) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionCard(
                    title = "🎯 Quiz Test",
                    subtitle = "5-8 Q&A questions",
                    modifier = Modifier.weight(1f),
                    onClick = { onActionClick(QuickAction.QA) }
                )
                ActionCard(
                    title = "🔍 Explain Terms",
                    subtitle = "Simplify complex jargon",
                    modifier = Modifier.weight(1f),
                    onClick = { onActionClick(QuickAction.EXPLAIN_TERMS) }
                )
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * User Chat Bubble with vibrant gradient and modern curvature.
 */
@Composable
private fun UserMessageBubble(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 4.dp
            ),
            shadowElevation = 2.dp,
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 4.dp
                    )
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(BrandPurpleDeep, BrandPurpleEnd)
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BrandPurpleDeep, BrandPurpleEnd)
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * AI Response Bubble with card styling, AI badge, copy action, and thinking shimmer.
 */
@Composable
private fun AiResponseBubble(
    text: String,
    isStreaming: Boolean,
    onCopy: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = 4.dp,
                bottomEnd = 18.dp
            ),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header with AI Sparkle badge and Copy button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEDE9FE),
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = BrandPurpleDeep,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Assistant",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPurpleDeep
                        )
                    }

                    if (text.isNotEmpty()) {
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Content or Thinking Animation
                if (text.isEmpty() && isStreaming) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = BrandPurpleDeep,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Reading & generating response...",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                } else {
                    SelectionContainer {
                        Text(
                            text = text,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
