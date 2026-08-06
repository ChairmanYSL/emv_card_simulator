package com.szzt.cardsimulator.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.szzt.cardsimulator.keymgmt.api.KeyImporter
import com.szzt.cardsimulator.keymgmt.api.KeyStore
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val keyStore: KeyStore,
    private val keyImporter: KeyImporter
) : ViewModel() {

    // TODO: Expose key IDs via StateFlow

    fun importKeys(json: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = keyImporter.importFromJson(json)
            onResult(
                "Imported: ${result.symmetricKeysImported} symmetric, " +
                "${result.rsaKeyPairsImported} RSA pairs. " +
                (if (result.errors.isNotEmpty()) "Errors: ${result.errors.joinToString()}" else "")
            )
        }
    }
}
