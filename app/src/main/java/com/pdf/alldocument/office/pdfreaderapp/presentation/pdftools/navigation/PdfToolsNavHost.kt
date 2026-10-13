package com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.home.HomeScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.screens.FilesTabScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.screens.SettingsTabScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.screens.ToolsTabScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.tools.AddTextScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.tools.AnnotateScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.tools.CompressScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.tools.EditTextScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.lock.LockPdfScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.tools.PdfToImageScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.tools.PdfToLongImageScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.tools.RecycleBinScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.split.SplitPdfScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.tools.TextToPdfScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.unlock.UnlockPdfScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.merge.MergePdfScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.organize.OrganizePdfScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.sign.SignPdfScreen

@Composable
fun PdfToolsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onExit: (() -> Unit)? = null
) {
    val back: () -> Unit = { navController.popBackStack() }
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigate = { navController.navigate(it.route) },
                onOpenTab = { navController.navigate(it.route) { popUpTo(Screen.Home.route) } },
                onBack = onExit
            )
        }
        composable(Screen.Files.route) { FilesTabScreen() }
        composable(Screen.Tools.route) {
            ToolsTabScreen(onToolClick = { navController.navigate(it.route) })
        }
        composable(Screen.Settings.route) { SettingsTabScreen() }

        composable(Screen.TextToPdf.route) { TextToPdfScreen(onBack = back) }
        composable(Screen.PdfToImage.route) { PdfToImageScreen(onBack = back) }
        composable(Screen.PdfToLongImage.route) { PdfToLongImageScreen(onBack = back) }
        composable(Screen.EditText.route) { EditTextScreen(onBack = back) }
        composable(Screen.AddText.route) { AddTextScreen(onBack = back) }
        composable(Screen.Annotate.route) { AnnotateScreen(onBack = back) }
        composable(Screen.Sign.route) {
            SignPdfScreen(
                filePath = "",
                title = "Sign PDF",
                onNavigateBack = back,
                onNavigateToViewer = { _, _ -> back() }
            )
        }
        composable(Screen.RecycleBin.route) { RecycleBinScreen(onBack = back) }
        composable(Screen.Merge.route) {
            MergePdfScreen(
                onNavigateBack = back,
                onNavigateToViewer = { _, _ -> back() }
            )
        }
        composable(Screen.Split.route) { SplitPdfScreen(onBack = back) }
        composable(Screen.ManagePages.route) {
            OrganizePdfScreen(
                filePath = "",
                title = "Manage Pages",
                onNavigateBack = back,
                onNavigateToViewer = { _, _ -> back() }
            )
        }
        composable(Screen.Compress.route) { CompressScreen(onBack = back) }
        composable(Screen.Lock.route) { LockPdfScreen(onBack = back) }
        composable(Screen.Unlock.route) { UnlockPdfScreen(onBack = back) }
    }
}
