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
        observeLockSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                // Load user info
                val currentUser = auth.currentUserOrNull()
                val userName = currentUser?.let { extractUserName(it) } ?: ""
                val userEmail = currentUser?.email ?: ""

                // Load role info
                val role = UserRole.fromString(sessionPreferences.getRole())
                val isOwner = role.isOwner()
                val userRoleDisplay = if (isOwner) "Pemilik" else "Kasir"

                // Load biometric availability
                val biometricAvailable = biometricManager.isBiometricAvailable()

                // Load lock settings
                val lockEnabled = appLockPreferences.isLockEnabledNow()
                val biometricEnabled = appLockPreferences.isBiometricEnabledNow()
                val pinSet = appLockPreferences.isPinSetNow()

                // Load access control settings (for owner)
                val ownerPinSet = try { accessControlManager.isOwnerPinSetNow() } catch (_: Exception) { false }
                val lockableRoutes = try {
                    accessControlManager.getLockableRoutesWithStatus().first()
                } catch (_: Exception) {
                    emptyList()
                }

                // Load sync info
                val pendingCount = try { syncManager.getPendingCount() } catch (_: Exception) { 0 }
                val lastSync = try { syncManager.getLastSyncTimestamp() } catch (_: Exception) { 0L }
                val lastSyncInfo = formatLastSync(lastSync)

                _uiState.update { state ->
                    state.copy(
                        userName = userName,
                        userEmail = userEmail,
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
            } catch (e: Exception) {
                Timber.e(e, "Error loading settings")
            }
        }
    }

    private fun observeAccessControl() {
        viewModelScope.launch {
            try {
                accessControlManager.getLockableRoutesWithStatus().collect { routes ->
                    _uiState.update { it.copy(lockableRoutes = routes) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error observing routes")
            }
        }
        viewModelScope.launch {
            try {
                accessControlManager.isOwnerPinSet().collect { isSet ->
                    _uiState.update { it.copy(ownerPinSet = isSet) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error observing owner pin")
            }
        }
    }

    private fun observeLockSettings() {
        viewModelScope.launch {
            appLockPreferences.isPinSet().collect { isSet ->
                _uiState.update { it.copy(pinSet = isSet) }
            }
        }
        viewModelScope.launch {
            appLockPreferences.isLockEnabled().collect { isEnabled ->
                _uiState.update { it.copy(lockEnabled = isEnabled) }
            }
        }
        viewModelScope.launch {
            appLockPreferences.isBiometricEnabled().collect { isEnabled ->
                _uiState.update { it.copy(biometricEnabled = isEnabled) }
            }
        }
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            try {
                syncManager.isSyncing().collect { isSyncing ->
                    _uiState.update { it.copy(isSyncing = isSyncing) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error observing sync status")
            }
        }
    }

    private fun extractUserName(user: UserInfo): String {
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
            // If disabling lock, also disable biometric
            if (!enabled) {
                appLockPreferences.setBiometricEnabled(false)
            }
            Timber.d("Lock enabled: $enabled")
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appLockPreferences.setBiometricEnabled(enabled)
            Timber.d("Biometric enabled: $enabled")
        }
    }

    fun refreshPinStatus() {
        viewModelScope.launch {
            val pinSet = appLockPreferences.isPinSetNow()
            val lockEnabled = appLockPreferences.isLockEnabledNow()
            _uiState.update { it.copy(pinSet = pinSet, lockEnabled = lockEnabled) }
        }
    }

    fun refreshOwnerPinStatus() {
        viewModelScope.launch {
            val ownerPinSet = accessControlManager.isOwnerPinSetNow()
            _uiState.update { it.copy(ownerPinSet = ownerPinSet) }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            try {
                syncManager.syncNow()
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
                appLockPreferences.resetAllSettings()
                sessionPreferences.clearSession()
                _uiState.update { it.copy(isLoggedOut = true) }
                Timber.d("User logged out")
            } catch (e: Exception) {
                Timber.e(e, "Logout failed")
            }
        }
    }

    fun toggleRouteLock(route: LockableRoute) {
        viewModelScope.launch {
            if (!_uiState.value.isOwner) return@launch
            accessControlManager.toggleRouteLock(route)
        }
    }

    fun setOwnerPin(pin: String) {
        viewModelScope.launch {
            if (!_uiState.value.isOwner) return@launch
            accessControlManager.setOwnerPin(pin)
            _uiState.update { it.copy(ownerPinSet = true) }
        }
    }
}
