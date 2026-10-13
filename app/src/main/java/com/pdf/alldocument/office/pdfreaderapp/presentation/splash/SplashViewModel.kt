package com.pdf.alldocument.office.pdfreaderapp.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.domain.repository.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashNavigationTarget {
    data object Loading : SplashNavigationTarget
    data object PrivacyPolicy : SplashNavigationTarget
    data object Language : SplashNavigationTarget
    data object Onboarding : SplashNavigationTarget
    data object Home : SplashNavigationTarget
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository,
    val interstitialAdHelper: com.pdf.alldocument.office.pdfreaderapp.core.ads.InterstitialAdHelper
) : ViewModel() {

    private val _navigationTarget = MutableStateFlow<SplashNavigationTarget>(SplashNavigationTarget.Loading)
    val navigationTarget: StateFlow<SplashNavigationTarget> = _navigationTarget.asStateFlow()

    init {
        determineDestination()
    }

    private fun determineDestination() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(2200) // Wait for splash animations to complete (progress: 150ms + 1900ms = 2050ms)
            val isLanguageSelected = userPrefsRepository.isLanguageSelected.first()
            val isPrivacyAccepted = userPrefsRepository.isPrivacyPolicyAccepted.first()
            val isOnboardingCompleted = userPrefsRepository.isOnboardingCompleted.first()

            if (!isLanguageSelected) {
                _navigationTarget.value = SplashNavigationTarget.Language
            } else if (!isOnboardingCompleted) {
                _navigationTarget.value = SplashNavigationTarget.Onboarding
            } else if (!isPrivacyAccepted) {
                _navigationTarget.value = SplashNavigationTarget.PrivacyPolicy
            } else {
                _navigationTarget.value = SplashNavigationTarget.Home
            }
        }
    }
}
