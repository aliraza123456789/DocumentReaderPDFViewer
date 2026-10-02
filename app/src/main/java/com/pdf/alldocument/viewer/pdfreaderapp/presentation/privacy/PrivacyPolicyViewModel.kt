package com.pdf.alldocument.viewer.pdfreaderapp.presentation.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.viewer.pdfreaderapp.domain.repository.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacyPolicyViewModel @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository
) : ViewModel() {

    val isAccepted: StateFlow<Boolean> = userPrefsRepository.isPrivacyPolicyAccepted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun acceptPrivacyPolicy(onComplete: () -> Unit) {
        viewModelScope.launch {
            userPrefsRepository.setPrivacyPolicyAccepted(true)
            onComplete()
        }
    }
}
