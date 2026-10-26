package com.pdf.alldocument.office.pdfreaderapp.presentation.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WorkspacePremium
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.BrandPurpleDeep
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.AccentGold
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.AppTopBar
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.ColorAll
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.PrimaryRed
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.SecondaryTeal

private data class MoreOption(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color,
    val onClick: () -> Unit
)

@Composable
fun MoreScreen(
    onNavigateToFileManager: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity
    val consentManager = androidx.compose.runtime.remember {
        com.pdf.alldocument.office.pdfreaderapp.core.ads.GoogleMobileAdsConsentManager.getInstance(context)
    }

    val options = buildList {
        add(
            MoreOption(
                title = "File Manager",
                description = "Browse, filter and sort all your documents",
                icon = Icons.Filled.FolderOpen,
                iconColor = PrimaryRed,
                onClick = onNavigateToFileManager
            )
        )
        add(
            MoreOption(
                title = "PDF Tools",
                description = "Merge, sign, organize and manage PDF files",
                icon = Icons.Filled.Build,
                iconColor = PrimaryRed,
                onClick = onNavigateToTools
            )
        )
        add(
            MoreOption(
                title = "Search Documents",
                description = "Find any PDF or office document on your device",
                icon = Icons.Filled.Search,
                iconColor = ColorAll,
                onClick = onNavigateToSearch
            )
        )
        add(
            MoreOption(
                title = "Premium",
                description = "Remove ads and unlock all document tools",
                icon = Icons.Filled.WorkspacePremium,
                iconColor = AccentGold,
                onClick = onNavigateToPremium
            )
        )
        add(
            MoreOption(
                title = "Language",
                description = "Change the app display language",
                icon = Icons.Filled.Language,
                iconColor = SecondaryTeal,
                onClick = onNavigateToLanguage
            )
        )
        add(
            MoreOption(
                title = "Privacy Policy",
                description = "Learn how your documents remain 100% private and local",
                icon = Icons.Filled.Security,
                iconColor = BrandPurpleDeep,
                onClick = onNavigateToPrivacyPolicy
            )
        )
        if (consentManager.isPrivacyOptionsRequired && activity != null) {
            add(
                MoreOption(
                    title = "Ad Privacy Settings",
                    description = "Manage your advertising and tracking consent choices",
                    icon = Icons.Filled.Security,
                    iconColor = SecondaryTeal,
                    onClick = {
                        consentManager.showPrivacyOptionsForm(activity) { _ -> }
                    }
                )
            )
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(title = "More")
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "Everything else",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "Tools and app settings in one place",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(options, key = { it.title }) { option ->
                MoreOptionCard(option = option)
            }
        }
    }
}

@Composable
private fun MoreOptionCard(option: MoreOption) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = option.onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(option.iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint = option.iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(3.dp))
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
