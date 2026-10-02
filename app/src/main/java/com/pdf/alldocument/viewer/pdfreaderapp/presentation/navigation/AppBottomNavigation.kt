package com.pdf.alldocument.viewer.pdfreaderapp.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdf.alldocument.viewer.pdfreaderapp.core.designsystem.BrandPurple

enum class AppBottomDestination(
    val route: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(
        route = Screen.Home,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    RECENT(
        route = Screen.Recent,
        label = "Recent",
        selectedIcon = Icons.Outlined.Schedule,
        unselectedIcon = Icons.Outlined.Schedule
    ),
    FAVOURITES(
        route = Screen.Favorites,
        label = "Favourite",
        selectedIcon = Icons.Outlined.StarOutline,
        unselectedIcon = Icons.Outlined.StarOutline
    ),
    TOOLS(
        route = Screen.ToolsHome,
        label = "Tools",
        selectedIcon = Icons.Outlined.GridView,
        unselectedIcon = Icons.Outlined.GridView
    )
}

/**
 * Modern Floating Island Dock Bottom Navigation.
 * Distinct from generic competitor clone bars:
 * - Floating rounded card with soft ambient elevation
 * - Capsule tonal pill highlight around active icon & text
 * - No competitor-style underline strip
 */
@Composable
fun AppBottomNavigation(
    selectedDestination: AppBottomDestination,
    onDestinationSelected: (AppBottomDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = Color(0x224F46E5)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppBottomDestination.entries.forEach { destination ->
                val isSelected = destination == selectedDestination
                val interactionSource = remember { MutableInteractionSource() }

                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) BrandPurple else Color(0xFF94A3B8),
                    animationSpec = tween(100),
                    label = "iconTint"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) Color(0xFFEEF2FF) else Color.Transparent
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onDestinationSelected(destination) }
                        )
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = destination.label,
                            modifier = Modifier.size(22.dp),
                            tint = iconTint
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = destination.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = iconTint
                        )
                    }
                }
            }
        }
    }
}
