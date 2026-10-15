package com.pdf.alldocument.office.pdfreaderapp.presentation.privacy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.AppTopBar
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.BrandPurple
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.BrandPurpleDeep
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.BrandPurpleEnd

private data class PrivacyHighlight(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private data class PolicySection(
    val icon: ImageVector,
    val title: String,
    val summary: String,
    val details: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrivacyPolicyScreen(
    isFirstLaunch: Boolean,
    viewModel: PrivacyPolicyViewModel = hiltViewModel(),
    onAgreeAndContinue: () -> Unit,
    onBack: () -> Unit
) {
    var isAgreed by remember { mutableStateOf(true) }
    var expandedSectionIndex by remember { mutableStateOf<Int?>(null) }

    val highlights = remember {
        listOf(
            PrivacyHighlight(
                icon = Icons.Default.CloudOff,
                title = "Zero Cloud Uploads",
                description = "Your documents and scans stay entirely on your phone."
            ),
            PrivacyHighlight(
                icon = Icons.Default.Lock,
                title = "100% Offline Processing",
                description = "PDF reading, conversion, and edits happen locally."
            ),
            PrivacyHighlight(
                icon = Icons.Default.NoAccounts,
                title = "No Account Required",
                description = "No registration, name, email, or credentials collected."
            ),
            PrivacyHighlight(
                icon = Icons.Default.Security,
                title = "Full User Control",
                description = "Revoke device permissions anytime in Android Settings."
            )
        )
    }

    val policySections = remember {
        listOf(
            PolicySection(
                icon = Icons.Default.Description,
                title = "1. Document & File Handling",
                summary = "Your files are never transmitted, analyzed, or shared.",
                details = "Document Reader processes PDF, Word (DOC/DOCX), Excel (XLS/XLSX), PowerPoint (PPT/PPTX), and Text files strictly within local memory. The contents of your files are read only to display them on your screen and to perform user-requested actions (such as merge, organize, sign, or convert). We never transfer or back up your files to any external server or cloud."
            ),
            PolicySection(
                icon = Icons.Default.FolderShared,
                title = "2. Device Storage Access",
                summary = "Used strictly to index and open documents on your phone.",
                details = "To serve as an all-in-one document suite, the app requests storage access (e.g. Media or External Storage permissions depending on your Android version). This permission is used exclusively to discover, list, search, and view your documents. The app does not access any unrelated private files."
            ),
            PolicySection(
                icon = Icons.Default.CameraAlt,
                title = "3. Camera & Scanner Usage",
                summary = "Active only when you choose to scan or take a photo.",
                details = "Camera permission is requested only when you launch 'Scan to PDF', 'ID Card Scanner', or 'ID Photo Maker'. Real-time camera feeds and captured photos are processed entirely on-device and saved directly to your local file system. We do not stream or upload your camera data."
            ),
            PolicySection(
                icon = Icons.Default.Info,
                title = "4. Third-Party Services & Ads",
                summary = "Standard non-invasive ads to support free features.",
                details = "The free tier of Document Reader uses Google AdMob to deliver ads. AdMob may utilize anonymous advertising identifiers and diagnostic signals in accordance with Google Play Developer Policy. If you upgrade to Premium, all ads are completely disabled. In-app purchases are safely processed through Google Play Billing without exposing payment credentials to us."
            ),
            PolicySection(
                icon = Icons.Default.VerifiedUser,
                title = "5. Diagnostics & Crash Reporting",
                summary = "Anonymous technical logs used only to fix bugs.",
                details = "To ensure high stability and fix crashes, we may collect anonymous crash statistics (such as Android OS version, device model, and crash trace). These diagnostics never contain your file contents, document names, or personal identities."
            ),
            PolicySection(
                icon = Icons.Default.Security,
                title = "6. Data Retention & Your Rights",
                summary = "You have complete authority over your permissions and data.",
                details = "Because we do not store your documents or profile on remote servers, deleting files or clearing the app data removes all cached information immediately. You can revoke storage and camera permissions at any time via Android Settings > Apps > Document Reader."
            )
        )
    }

    Scaffold(
        topBar = {
            if (!isFirstLaunch) {
                AppTopBar(
                    title = "Privacy Policy",
                    onBackClick = onBack
                )
            }
        },
        bottomBar = {
            if (isFirstLaunch) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isAgreed = !isAgreed }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isAgreed,
                                onCheckedChange = { isAgreed = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = BrandPurpleDeep,
                                    checkmarkColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "I have read and agree to the Privacy Policy & local processing terms.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (isAgreed) {
                                    viewModel.acceptPrivacyPolicy(onAgreeAndContinue)
                                }
                            },
                            enabled = isAgreed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandPurpleDeep,
                                disabledContainerColor = BrandPurpleDeep.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Agree & Continue",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Continue",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header for First Launch or Reader Mode
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(BrandPurple, BrandPurpleEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Privacy Shield",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCFCE7))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "100% Device-Local & Secure",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isFirstLaunch) "Your Privacy Matters" else "Privacy Policy",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Document Reader is built with privacy by design. We believe your documents and files are yours alone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }

            // Highlights Grid
            item {
                Text(
                    text = "Key Guarantees",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 2,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    highlights.forEach { item ->
                        PrivacyHighlightCard(
                            highlight = item,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            // Detailed Policy Accordion Sections
            item {
                Text(
                    text = "Detailed Policy Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            itemsIndexed(policySections) { index, section ->
                val isExpanded = expandedSectionIndex == index
                PolicyAccordionCard(
                    section = section,
                    isExpanded = isExpanded,
                    onToggle = {
                        expandedSectionIndex = if (isExpanded) null else index
                    }
                )
            }

            // Trust Footer Note
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = BrandPurpleDeep,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Last updated: October 2026. This app is compliant with Google Play Developer Policies and global privacy regulations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            if (!isFirstLaunch) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurpleDeep)
                    ) {
                        Text(text = "Back", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyHighlightCard(
    highlight: PrivacyHighlight,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandPurple.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = highlight.icon,
                    contentDescription = null,
                    tint = BrandPurpleDeep,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = highlight.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = highlight.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PolicyAccordionCard(
    section: PolicySection,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(BrandPurpleDeep.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = section.icon,
                        contentDescription = null,
                        tint = BrandPurpleDeep,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = section.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = section.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}
