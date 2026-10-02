package com.pdf.alldocument.viewer.pdfreaderapp.core.localization

data class SupportedLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flagEmoji: String
)

val APP_SUPPORTED_LANGUAGES = listOf(
    SupportedLanguage("en", "English", "English", "🇺🇸"),
    SupportedLanguage("es", "Spanish", "Español", "🇪🇸"),
    SupportedLanguage("fr", "French", "Français", "🇫🇷"),
    SupportedLanguage("de", "German", "Deutsch", "🇩🇪"),
    SupportedLanguage("it", "Italian", "Italiano", "🇮🇹"),
    SupportedLanguage("pt", "Portuguese", "Português", "🇵🇹"),
    SupportedLanguage("ru", "Russian", "Русский", "🇷🇺"),
    SupportedLanguage("zh", "Chinese", "中文", "🇨🇳"),
    SupportedLanguage("ja", "Japanese", "日本語", "🇯🇵"),
    SupportedLanguage("hi", "Hindi", "हिन्दी", "🇮🇳"),
    SupportedLanguage("ar", "Arabic", "العربية", "🇸🇦"),
    SupportedLanguage("in", "Indonesian", "Bahasa Indonesia", "🇮🇩"),
    SupportedLanguage("tr", "Turkish", "Türkçe", "🇹🇷")
)
