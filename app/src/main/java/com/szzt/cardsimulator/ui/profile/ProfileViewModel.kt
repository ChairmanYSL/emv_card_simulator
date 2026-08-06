package com.szzt.cardsimulator.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.szzt.cardsimulator.profile.api.ProfileRepository
import com.szzt.cardsimulator.profile.model.CardProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: ProfileRepository
) : ViewModel() {

    val profiles: StateFlow<List<CardProfile>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfile: StateFlow<CardProfile?> = repository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setActive(profileId: String) {
        viewModelScope.launch {
            repository.setActive(profileId)
        }
    }

    fun delete(profileId: String) {
        viewModelScope.launch {
            repository.delete(profileId)
        }
    }

    fun importFromJson(json: String) {
        viewModelScope.launch {
            repository.importFromJson(json)
        }
    }

    fun exportToJson(profileId: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportToJson(profileId)
            onResult(json)
        }
    }
}
