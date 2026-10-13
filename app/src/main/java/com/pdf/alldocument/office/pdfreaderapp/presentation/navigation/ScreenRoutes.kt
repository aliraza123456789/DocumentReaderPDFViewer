package com.pdf.alldocument.office.pdfreaderapp.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {

    @Serializable
    data object Splash : Screen

    @Serializable
    data object Onboarding : Screen

    @Serializable
    data class PrivacyPolicy(val isFirstLaunch: Boolean = false) : Screen

    @Serializable
    data class LanguageSelection(val isFirstLaunch: Boolean = false) : Screen

    @Serializable
    data object Home : Screen

    @Serializable
    data object FileManager : Screen

    @Serializable
    data object Favorites : Screen

    @Serializable
    data object Recent : Screen

    @Serializable
    data object More : Screen

    @Serializable
    data class GlobalSearch(val initialType: String = "ALL") : Screen

    @Serializable
    data class PdfViewer(val filePath: String = "", val title: String = "") : Screen

    @Serializable
    data class OfficeViewer(val filePath: String = "", val title: String = "") : Screen

    @Serializable
    data object ToolsHome : Screen

    @Serializable
    data object PdfToolsHome : Screen

    @Serializable
    data object MergePdf : Screen

    @Serializable
    data class SignPdf(val filePath: String, val title: String) : Screen

    @Serializable
    data class OrganizePdf(val filePath: String, val title: String) : Screen

    @Serializable
    data object TextToPdf : Screen

    @Serializable
    data object PdfToImage : Screen

    @Serializable
    data object PdfToLongImage : Screen

    @Serializable
    data object LockPdf : Screen

    @Serializable
    data object UnlockPdf : Screen

    @Serializable
    data object SplitPdf : Screen

    @Serializable
    data object CompressPdf : Screen

    @Serializable
    data object ImageToPdf : Screen

    @Serializable
    data object ScanToPdf : Screen

    @Serializable
    data object ImageToText : Screen

    @Serializable
    data object IdCardScanner : Screen

    @Serializable
    data object IdPhotoMaker : Screen

    @Serializable
    data object AiReading : Screen

    @Serializable
    data object ScanToExcel : Screen

    @Serializable
    data object OfficeToPdf : Screen

    @Serializable
    data object WordToPdf : Screen

    @Serializable
    data object PdfToWord : Screen

    @Serializable
    data class Premium(val isFromOnboarding: Boolean = false) : Screen
}
