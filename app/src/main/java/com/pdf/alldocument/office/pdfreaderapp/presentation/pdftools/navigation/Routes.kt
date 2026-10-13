package com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Files : Screen("files")
    data object Tools : Screen("tools")
    data object Settings : Screen("settings")

    // Convert
    data object TextToPdf : Screen("text_to_pdf")
    data object PdfToImage : Screen("pdf_to_image")
    data object PdfToLongImage : Screen("pdf_to_long_image")

    // Edit
    data object EditText : Screen("edit_text")
    data object AddText : Screen("add_text")
    data object Annotate : Screen("annotate")
    data object Sign : Screen("sign")

    // Manage
    data object RecycleBin : Screen("recycle_bin")

    // Other
    data object Merge : Screen("merge")
    data object Split : Screen("split")
    data object ManagePages : Screen("manage_pages")
    data object Compress : Screen("compress")
    data object Lock : Screen("lock")
    data object Unlock : Screen("unlock")
}
