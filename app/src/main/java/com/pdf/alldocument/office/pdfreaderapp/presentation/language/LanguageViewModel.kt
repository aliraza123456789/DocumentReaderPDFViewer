package com.pdf.alldocument.office.pdfreaderapp.presentation.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.core.localization.APP_SUPPORTED_LANGUAGES
import com.pdf.alldocument.office.pdfreaderapp.core.localization.SupportedLanguage
import com.pdf.alldocument.office.pdfreaderapp.domain.repository.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository,
    val interstitialAdHelper: com.pdf.alldocument.office.pdfreaderapp.core.ads.InterstitialAdHelper
) : ViewModel() {

    val languages: List<SupportedLanguage> = APP_SUPPORTED_LANGUAGES

    val selectedLanguageCode: StateFlow<String> = userPrefsRepository.languageCode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    val isLanguageSelected: StateFlow<Boolean> = userPrefsRepository.isLanguageSelected
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun selectLanguage(code: String) {
        viewModelScope.launch {
            saveLanguageSuspend(code)
        }
    }

    suspend fun saveLanguageSuspend(code: String) {
        userPrefsRepository.setLanguageCode(code)
        userPrefsRepository.setIsLanguageSelected(true)
    }
}
