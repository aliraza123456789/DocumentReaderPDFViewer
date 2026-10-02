package com.pdf.alldocument.viewer.pdfreaderapp.presentation.pdftools.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.pdf.alldocument.viewer.pdfreaderapp.core.designsystem.DocumentReaderTheme

/**
 * The PDF tools subtree used to define its own blue theme with
 * `dynamicColor = true`, which meant the tools screens rendered in a different
 * brand than the rest of the app — and in the *wallpaper* palette on Android
 * 12+. That is now delegated to the shared [DocumentReaderTheme] so the whole
 * app is one purple brand in both light and dark.
 *
 * The signature is kept so existing call sites compile unchanged.
 */
@Composable
fun PdfToolsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    DocumentReaderTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
