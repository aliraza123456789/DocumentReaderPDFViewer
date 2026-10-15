package com.pdf.alldocument.office.pdfreaderapp.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdf.alldocument.office.pdfreaderapp.R

enum class AppBottomDestination(
    val route: Screen,
    val label: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector,
    val selectedDrawableRes: Int? = null
) {
    HOME(
        route = Screen.Home,
        label = "Home",
        selectedDrawableRes = R.drawable.home,
        unselectedIcon = Icons.Outlined.Home
    ),
    RECENT(
        route = Screen.Recent,
        label = "Recent",
        selectedIcon = Icons.Filled.Schedule,
        unselectedIcon = Icons.Outlined.Schedule
    ),
    FAVOURITES(
        route = Screen.Favorites,
        label = "Favourite",
        selectedIcon = Icons.Filled.Star,
        unselectedIcon = Icons.Outlined.StarOutline
    ),
    TOOLS(
        route = Screen.ToolsHome,
        label = "Tools",
        selectedIcon = Icons.Filled.GridView,
        unselectedIcon = Icons.Outlined.GridView
    )
}

/**
 * Bottom navigation bar matching the UI:
 * - Selected tab: FILLED icon, GREEN color (#22C55E), semi-bold text
 * - Unselected tab: OUTLINED icon, GREY color (#9CA3AF), normal text
 * - Fixes Home tab staying green all the time.
 */
@Composable
fun AppBottomNavigation(
    selectedDestination: AppBottomDestination,
    onDestinationSelected: (AppBottomDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeGreen = Color(0xFF22C55E)
    val inactiveGrey = Color(0xFF9CA3AF)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
    ) {
        // Thin top divider
        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.8.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppBottomDestination.entries.forEach { destination ->
                val isSelected = destination == selectedDestination
                val interactionSource = remember { MutableInteractionSource() }

                val itemColor = if (isSelected) activeGreen else inactiveGrey

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onDestinationSelected(destination) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (destination == AppBottomDestination.HOME && isSelected && destination.selectedDrawableRes != null) {
                        // Home selected: Use custom filled SVG home icon tinted green
                        Icon(
                            painter = painterResource(id = destination.selectedDrawableRes),
                            contentDescription = destination.label,
                            modifier = Modifier.size(24.dp),
                            tint = itemColor
                        )
                    } else {
                        // Outlined icon when unselected, filled icon when selected
                        val iconVector = if (isSelected) {
                            destination.selectedIcon ?: destination.unselectedIcon
                        } else {
                            destination.unselectedIcon
                        }
                        Icon(
                            imageVector = iconVector,
                            contentDescription = destination.label,
                            modifier = Modifier.size(24.dp),
                            tint = itemColor
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = destination.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = itemColor
                    )
                }
            }
        }
    }
}

