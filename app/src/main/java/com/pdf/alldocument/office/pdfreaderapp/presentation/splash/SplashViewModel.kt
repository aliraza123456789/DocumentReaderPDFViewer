package com.pdf.alldocument.office.pdfreaderapp.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.core.ads.AdMobManager
import com.pdf.alldocument.office.pdfreaderapp.core.ads.GoogleMobileAdsConsentManager
import com.pdf.alldocument.office.pdfreaderapp.core.ads.InterstitialAdHelper
import com.pdf.alldocument.office.pdfreaderapp.domain.repository.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
    val interstitialAdHelper: InterstitialAdHelper,
    val adMobManager: AdMobManager,
    val consentManager: GoogleMobileAdsConsentManager
) : ViewModel() {

    private val _navigationTarget = MutableStateFlow<SplashNavigationTarget>(SplashNavigationTarget.Loading)
    val navigationTarget: StateFlow<SplashNavigationTarget> = _navigationTarget.asStateFlow()

    private var isAnimationTimerFinished = false
    private var isConsentFinished = false

    init {
        startSplashTimer()
    }

    private fun startSplashTimer() {
        viewModelScope.launch {
            delay(2200) // Wait for splash entrance and progress animation (progress: 150ms + 1900ms = 2050ms)
            isAnimationTimerFinished = true
            checkReadyToNavigate()
        }
    }

    fun onConsentGathered() {
        isConsentFinished = true
        checkReadyToNavigate()
    }

    private fun checkReadyToNavigate() {
        if (!isAnimationTimerFinished || !isConsentFinished) return

        viewModelScope.launch {
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
