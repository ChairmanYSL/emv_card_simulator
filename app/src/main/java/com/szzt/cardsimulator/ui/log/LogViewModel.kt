package com.szzt.cardsimulator.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.szzt.cardsimulator.log.api.ApduLogEntry
import com.szzt.cardsimulator.log.api.ApduLogger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogViewModel(
    private val logger: ApduLogger
) : ViewModel() {

    val logEntries: StateFlow<List<ApduLogEntry>> = logger.observe()
        .scan(emptyList<ApduLogEntry>()) { list, entry -> list + entry }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clear() {
        viewModelScope.launch {
            logger.clear()
        }
    }
}
