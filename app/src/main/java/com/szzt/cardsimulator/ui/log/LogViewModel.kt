package com.szzt.cardsimulator.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.szzt.cardsimulator.log.api.ApduLogEntry
import com.szzt.cardsimulator.log.api.ApduLogger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LogViewModel(
    private val logger: ApduLogger
) : ViewModel() {

    val logEntries: StateFlow<List<ApduLogEntry>> = logger.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clear() {
        logger.clear()
    }
}
