package com.zatiaras.pos.feature.printer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zatiaras.pos.feature.printer.domain.model.PaperWidth
import com.zatiaras.pos.feature.printer.domain.model.PrinterDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.printerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "printer_preferences"
)

/**
 * DataStore preferences for printer settings.
 */
@Singleton
class PrinterPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_LAST_PRINTER_NAME = stringPreferencesKey("last_printer_name")
        private val KEY_LAST_PRINTER_ADDRESS = stringPreferencesKey("last_printer_address")
        private val KEY_PAPER_WIDTH = intPreferencesKey("paper_width")
        private val KEY_STORE_NAME = stringPreferencesKey("store_name")
        private val KEY_STORE_ADDRESS = stringPreferencesKey("store_address")
        private val KEY_AUTO_CONNECT = stringPreferencesKey("auto_connect")
    }
    
    private val dataStore = context.printerDataStore
    
    // ==================== LAST PRINTER ====================
    
    /**
     * Save the last connected printer.
     */
    suspend fun saveLastPrinter(device: PrinterDevice) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_PRINTER_NAME] = device.name
            prefs[KEY_LAST_PRINTER_ADDRESS] = device.address
        }
    }
    
    /**
     * Get the last connected printer.
     */
    suspend fun getLastPrinter(): PrinterDevice? {
        val prefs = dataStore.data.first()
        val name = prefs[KEY_LAST_PRINTER_NAME] ?: return null
        val address = prefs[KEY_LAST_PRINTER_ADDRESS] ?: return null
        
        return PrinterDevice(
            name = name,
            address = address,
            isPaired = true
        )
    }
    
    /**
     * Observe the last printer as a Flow.
     */
    fun observeLastPrinter(): Flow<PrinterDevice?> = dataStore.data.map { prefs ->
        val name = prefs[KEY_LAST_PRINTER_NAME] ?: return@map null
        val address = prefs[KEY_LAST_PRINTER_ADDRESS] ?: return@map null
        
        PrinterDevice(name = name, address = address, isPaired = true)
    }
    
    /**
     * Clear the last printer.
     */
    suspend fun clearLastPrinter() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_LAST_PRINTER_NAME)
            prefs.remove(KEY_LAST_PRINTER_ADDRESS)
        }
    }
    
    // ==================== PAPER WIDTH ====================
    
    /**
     * Save paper width setting.
     */
    suspend fun savePaperWidth(width: PaperWidth) {
        dataStore.edit { prefs ->
            prefs[KEY_PAPER_WIDTH] = width.mmWidth
        }
    }
    
    /**
     * Get paper width setting.
     */
    suspend fun getPaperWidth(): PaperWidth {
        val prefs = dataStore.data.first()
        val width = prefs[KEY_PAPER_WIDTH] ?: PaperWidth.MM_58.mmWidth
        
        return if (width == 80) PaperWidth.MM_80 else PaperWidth.MM_58
    }
    
    /**
     * Observe paper width as a Flow.
     */
    fun observePaperWidth(): Flow<PaperWidth> = dataStore.data.map { prefs ->
        val width = prefs[KEY_PAPER_WIDTH] ?: PaperWidth.MM_58.mmWidth
        if (width == 80) PaperWidth.MM_80 else PaperWidth.MM_58
    }
    
    // ==================== STORE INFO ====================
    
    /**
     * Save store info for receipt header.
     */
    suspend fun saveStoreInfo(name: String, address: String?) {
        dataStore.edit { prefs ->
            prefs[KEY_STORE_NAME] = name
            address?.let { prefs[KEY_STORE_ADDRESS] = it }
        }
    }
    
    /**
     * Get store name.
     */
    suspend fun getStoreName(): String {
        val prefs = dataStore.data.first()
        return prefs[KEY_STORE_NAME] ?: "Zatiaras Juice"
    }
    
    /**
     * Get store address.
     */
    suspend fun getStoreAddress(): String? {
        val prefs = dataStore.data.first()
        return prefs[KEY_STORE_ADDRESS]
    }
    
    // ==================== AUTO CONNECT ====================
    
    /**
     * Enable/disable auto-connect to last printer.
     */
    suspend fun setAutoConnect(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_CONNECT] = if (enabled) "true" else "false"
        }
    }
    
    /**
     * Check if auto-connect is enabled.
     */
    suspend fun isAutoConnectEnabled(): Boolean {
        val prefs = dataStore.data.first()
        return prefs[KEY_AUTO_CONNECT] == "true"
    }
    
    // ==================== STORE LOGO ====================
    
    private val KEY_STORE_LOGO = stringPreferencesKey("store_logo_uri")
    
    /**
     * Save store logo URI.
     * Pass null to use default app logo.
     */
    suspend fun saveStoreLogo(uri: String?) {
        dataStore.edit { prefs ->
            if (uri != null) {
                prefs[KEY_STORE_LOGO] = uri
            } else {
                prefs.remove(KEY_STORE_LOGO)
            }
        }
    }
    
    /**
     * Get store logo URI.
     * Returns null if using default app logo.
     */
    suspend fun getStoreLogo(): String? {
        val prefs = dataStore.data.first()
        return prefs[KEY_STORE_LOGO]
    }
    
    /**
     * Clear store logo to use default.
     */
    suspend fun clearStoreLogo() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_STORE_LOGO)
        }
    }
}
