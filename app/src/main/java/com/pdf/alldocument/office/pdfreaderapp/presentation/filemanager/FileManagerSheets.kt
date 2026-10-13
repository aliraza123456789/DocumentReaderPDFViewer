package com.pdf.alldocument.office.pdfreaderapp.presentation.filemanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.ConfirmDeleteDialog
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.RenameDialog
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.domain.model.SortOrder

/* ------------------------------------------------------------------ */
/* Sort sheet                                                          */
/* ------------------------------------------------------------------ */

/**
 * Sort order picker. Uses `selectable` + `Role.RadioButton` so TalkBack
 * announces each row as a radio option and reports the checked one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    current: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            SheetTitle(text = "Sort by")
            Spacer(Modifier.height(4.dp))
            SortOrder.entries.forEach { order ->
                SortOptionRow(
                    order = order,
                    selected = order == current,
                    onClick = { onSortSelected(order) }
                )
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .semantics { heading() }
    )
}

@Composable
private fun SortOptionRow(
    order: SortOrder,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Explicit radio glyph — M3's RadioButton is 48dp and would bloat the row.
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (selected) {
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(PurpleStart, PurpleEnd)
                        )
                    } else {
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.Transparent)
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (selected) Color.White else Color.Transparent)
            )
        }
        Spacer(Modifier.size(14.dp))
        Text(
            text = order.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) PurpleStart else MaterialTheme.colorScheme.onSurface
        )
    }
}

/* ------------------------------------------------------------------ */
/* 3-dot item menu sheet                                               */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentMenuSheet(
    document: DocumentItem,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            SheetTitle(text = document.name)
            Spacer(Modifier.height(4.dp))

            MenuActionRow(
                icon = if (document.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                label = if (document.isFavorite) "Remove from favourites" else "Add to favourites",
                tint = if (document.isFavorite) CrownOrange else MaterialTheme.colorScheme.onSurface,
                onClick = onToggleFavorite
            )
            MenuActionRow(
                icon = Icons.Default.DriveFileRenameOutline,
                label = "Rename",
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = onRename
            )
            MenuActionRow(
                icon = Icons.Default.Delete,
                label = "Delete",
                tint = MaterialTheme.colorScheme.error,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun MenuActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}

/* ------------------------------------------------------------------ */
/* Rename + delete dialogs (shared with Home)                          */
/* ------------------------------------------------------------------ */

@Composable
fun FileManagerRenameDialog(
    document: DocumentItem,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // `FileUtils.renameFile` appends the original extension itself, so the field
    // must hold the base name only — passing "report.pdf" would produce
    // "report.pdf.pdf".
    RenameDialog(
        initialName = document.toFileItem().name,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun FileManagerDeleteDialog(
    document: DocumentItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ConfirmDeleteDialog(
        fileName = document.name,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}
