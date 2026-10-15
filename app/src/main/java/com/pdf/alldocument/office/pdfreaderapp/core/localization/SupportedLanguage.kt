package com.pdf.alldocument.office.pdfreaderapp.core.localization

import androidx.annotation.DrawableRes
import com.pdf.alldocument.office.pdfreaderapp.R

data class SupportedLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flagEmoji: String,
    @DrawableRes val flagRes: Int = 0
)

val APP_SUPPORTED_LANGUAGES = listOf(
    SupportedLanguage("en", "English", "English", "🇺🇸", R.drawable.flag_en),
    SupportedLanguage("es", "Spanish", "Español", "🇪🇸", R.drawable.flag_es),
    SupportedLanguage("fr", "French", "Français", "🇫🇷", R.drawable.flag_fr),
    SupportedLanguage("de", "German", "Deutsch", "🇩🇪", R.drawable.flag_de),
    SupportedLanguage("it", "Italian", "Italiano", "🇮🇹", R.drawable.flag_it),
    SupportedLanguage("pt", "Portuguese", "Português", "🇵🇹", R.drawable.flag_pt),
    SupportedLanguage("ru", "Russian", "Русский", "🇷🇺", R.drawable.flag_ru),
    SupportedLanguage("zh", "Chinese", "中文", "🇨🇳", R.drawable.flag_zh),
    SupportedLanguage("ja", "Japanese", "日本語", "🇯🇵", R.drawable.flag_ja),
    SupportedLanguage("hi", "Hindi", "हिन्दी", "🇮🇳", R.drawable.flag_hi),
    SupportedLanguage("ar", "Arabic", "العربية", "🇸🇦", R.drawable.flag_ar),
    SupportedLanguage("in", "Indonesian", "Bahasa Indonesia", "🇮🇩", R.drawable.flag_in),
    SupportedLanguage("tr", "Turkish", "Türkçe", "🇹🇷", R.drawable.flag_tr)
)
