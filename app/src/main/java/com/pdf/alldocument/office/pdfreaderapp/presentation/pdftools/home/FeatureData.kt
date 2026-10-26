package com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pages
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.ui.graphics.vector.ImageVector
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.navigation.Screen

data class Feature(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: Screen?
)

val convertFeatures = listOf(
    Feature("Text to PDF", "Text ko PDF banao", Icons.Filled.TextFields, Screen.TextToPdf),
    Feature("PDF to Image", "Pages ko JPG/PNG banao", Icons.Filled.Image, Screen.PdfToImage),
    Feature("PDF to Long Image", "Sab pages ek image", Icons.Filled.Pages, Screen.PdfToLongImage)
)

val editFeatures = listOf(
    Feature("Edit Text", "Existing text modify", Icons.AutoMirrored.Filled.Article, Screen.EditText),
    Feature("Add Text", "Naya text add karo", Icons.Filled.Add, Screen.AddText),
    Feature("Annotate", "Draw, highlight, notes", Icons.Filled.Brush, Screen.Annotate),
    Feature("Sign PDF", "Digital signature on PDF", Icons.Filled.Draw, Screen.Sign)
)

val manageFeatures = listOf(
    Feature("Import Files", "Device se import", Icons.Filled.PictureAsPdf, null),
    Feature("Create Folder", "Organize karo", Icons.Filled.CreateNewFolder, null),
    Feature("Recycle Bin", "Deleted files restore", Icons.Filled.Delete, Screen.RecycleBin),
    Feature("Print", "Printer se print", Icons.Filled.Print, null)
)

val otherFeatures = listOf(
    Feature("Merge PDF", "Multiple = 1 PDF", Icons.AutoMirrored.Filled.MergeType, Screen.Merge),
    Feature("Split PDF", "1 PDF = multiple", Icons.AutoMirrored.Filled.CallSplit, Screen.Split),
    Feature("Manage Pages", "Reorder, rotate, delete", Icons.Filled.Pages, Screen.ManagePages),
    Feature("Compress", "Size kam karo", Icons.Filled.Compress, Screen.Compress),
    Feature("Lock PDF", "Password protect", Icons.Filled.Lock, Screen.Lock),
    Feature("Unlock PDF", "Password hatao", Icons.Filled.LockOpen, Screen.Unlock)
)

val allFeatures = convertFeatures + editFeatures + manageFeatures + otherFeatures
