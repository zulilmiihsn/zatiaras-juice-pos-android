package com.zatiaras.pos.feature.auth.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.core.data.access.AccessControlManager
import com.zatiaras.pos.core.data.access.LockableRoute
import com.zatiaras.pos.core.data.access.UserRole
import com.zatiaras.pos.core.data.session.SessionPreferences
import com.zatiaras.pos.core.data.sync.SyncManager
import com.zatiaras.pos.feature.auth.lock.AppBiometricManager
import com.zatiaras.pos.feature.auth.lock.AppLockPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SettingsUiState(
    // Profile
    val userName: String = "",
    val userEmail: String = "",
    val userRole: String = "Kasir",
    val branchName: String = "Cabang Utama",
    
    // Security
    val lockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val pinSet: Boolean = false,
    
    // Access Control (Owner only)
    val isOwner: Boolean = false,
    val ownerPinSet: Boolean = false,
    val lockableRoutes: List<Pair<LockableRoute, Boolean>> = emptyList(),
    
    // Sync
    val lastSyncInfo: String = "Belum pernah sync",
    val pendingCount: Int = 0,
    val isSyncing: Boolean = false,
    
    // State
    val isLoggedOut: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: Auth,
    private val appLockPreferences: AppLockPreferences,
    private val biometricManager: AppBiometricManager,
    private val syncManager: SyncManager,
    private val accessControlManager: AccessControlManager,
    private val sessionPreferences: SessionPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeAccessControl()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // Load user info
            val currentUser = auth.currentUserOrNull()
            currentUser?.let { user ->
                _uiState.update { state ->
                    state.copy(
                        userEmail = user.email ?: "",
                        userName = extractUserName(user)
                    )
                }
            }

            // Load role info
            val role = UserRole.fromString(sessionPreferences.getRole())
            val isOwner = role.isOwner()
            val userRoleDisplay = if (isOwner) "Pemilik" else "Kasir"

            // Load lock settings
            val lockEnabled = appLockPreferences.isLockEnabledNow()
            val biometricEnabled = appLockPreferences.isBiometricEnabledNow()
            val biometricAvailable = biometricManager.isBiometricAvailable()
            val pinSet = appLockPreferences.isPinSetNow()

            // Load access control settings (for owner)
            val ownerPinSet = accessControlManager.isOwnerPinSetNow()
            val lockableRoutes = accessControlManager.getLockableRoutesWithStatus().first()

            // Load sync info
            val pendingCount = syncManager.getPendingCount()
            val lastSync = syncManager.getLastSyncTimestamp()
            val lastSyncInfo = formatLastSync(lastSync)

            _uiState.update { state ->
                state.copy(
                    userRole = userRoleDisplay,
                    isOwner = isOwner,
                    lockEnabled = lockEnabled,
                    biometricEnabled = biometricEnabled,
                    biometricAvailable = biometricAvailable,
                    pinSet = pinSet,
                    ownerPinSet = ownerPinSet,
                    lockableRoutes = lockableRoutes,
                    pendingCount = pendingCount,
                    lastSyncInfo = lastSyncInfo
                )
            }

            // Observe sync in progress
            observeSyncStatus()
        }
    }

    private fun observeAccessControl() {
        viewModelScope.launch {
            accessControlManager.getLockableRoutesWithStatus().collect { routes ->
                _uiState.update { it.copy(lockableRoutes = routes) }
            }
        }
        viewModelScope.launch {
            accessControlManager.isOwnerPinSet().collect { isSet ->
                _uiState.update { it.copy(ownerPinSet = isSet) }
            }
        }
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            syncManager.isSyncing().collect { isSyncing ->
                _uiState.update { it.copy(isSyncing = isSyncing) }
            }
        }
    }

    private fun extractUserName(user: UserInfo): String {
        // Try to get name from user metadata
        return user.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User"
    }

    private fun formatLastSync(timestamp: Long): String {
        if (timestamp == 0L) return "Belum pernah sync"
        
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "Baru saja"
            diff < 3600_000 -> "${diff / 60_000} menit lalu"
            diff < 86400_000 -> "${diff / 3600_000} jam lalu"
            else -> "${diff / 86400_000} hari lalu"
        }
    }

    fun setLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appLockPreferences.setLockEnabled(enabled)
            _uiState.update { it.copy(lockEnabled = enabled) }

            // If disabling lock, also disable biometric
            if (!enabled) {
                appLockPreferences.setBiometricEnabled(false)
                _uiState.update { it.copy(biometricEnabled = false) }
            }

            Timber.d("Lock enabled: $enabled")
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appLockPreferences.setBiometricEnabled(enabled)
            _uiState.update { it.copy(biometricEnabled = enabled) }
            Timber.d("Biometric enabled: $enabled")
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            
            try {
                syncManager.syncNow()
                
                // Refresh sync info
                val pendingCount = syncManager.getPendingCount()
                val lastSync = syncManager.getLastSyncTimestamp()
                
                _uiState.update { state ->
                    state.copy(
                        pendingCount = pendingCount,
                        lastSyncInfo = formatLastSync(lastSync)
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Sync failed")
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    fun forceFullSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            
            try {
                syncManager.forceFullSync()
                
                // Refresh sync info
                val pendingCount = syncManager.getPendingCount()
                val lastSync = syncManager.getLastSyncTimestamp()
                
                _uiState.update { state ->
                    state.copy(
                        pendingCount = pendingCount,
                        lastSyncInfo = formatLastSync(lastSync)
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Force full sync failed")
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                auth.signOut()
                // Clear lock settings on logout
                appLockPreferences.resetAllSettings()
                // Clear session
                sessionPreferences.clearSession()
                _uiState.update { it.copy(isLoggedOut = true) }
                Timber.d("User logged out")
            } catch (e: Exception) {
                Timber.e(e, "Logout failed")
            }
        }
    }

    // ==================== ACCESS CONTROL (Owner only) ====================

    /**
     * Toggle lock status for a route.
     * Only owner can call this.
     */
    fun toggleRouteLock(route: LockableRoute) {
        viewModelScope.launch {
            if (!_uiState.value.isOwner) {
                Timber.w("Non-owner tried to toggle route lock")
                return@launch
            }
            accessControlManager.toggleRouteLock(route)
            Timber.d("Toggled lock for route: ${route.displayName}")
        }
    }

    /**
     * Set owner PIN for access control.
     * Only owner can call this.
     */
    fun setOwnerPin(pin: String) {
        viewModelScope.launch {
            if (!_uiState.value.isOwner) {
                Timber.w("Non-owner tried to set owner PIN")
                return@launch
            }
            accessControlManager.setOwnerPin(pin)
            _uiState.update { it.copy(ownerPinSet = true) }
            Timber.d("Owner PIN set")
        }
    }
}
