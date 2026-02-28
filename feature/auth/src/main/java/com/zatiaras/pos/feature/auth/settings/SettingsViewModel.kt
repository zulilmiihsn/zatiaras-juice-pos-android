package com.zatiaras.pos.feature.auth.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.core.data.access.AccessControlManager
import com.zatiaras.pos.core.data.access.LockableRoute
import com.zatiaras.pos.core.data.access.UserRole
import com.zatiaras.pos.core.data.repository.AppSettingsRepository
import com.zatiaras.pos.core.data.session.SessionPreferences
import com.zatiaras.pos.core.data.sync.SyncManager
import com.zatiaras.pos.core.domain.Result
import com.zatiaras.pos.core.domain.usecase.ChangeCurrentUserPasswordUseCase
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

enum class PasswordChangeMessage {
    REQUIRED_FIELDS,
    MIN_LENGTH,
    CONFIRMATION_MISMATCH,
    SAME_AS_CURRENT,
    PASSWORD_CHANGED,
    GENERIC_FAILURE
}

enum class LastSyncUnit {
    NEVER,
    JUST_NOW,
    MINUTES_AGO,
    HOURS_AGO,
    DAYS_AGO
}

data class LastSyncInfo(
    val unit: LastSyncUnit,
    val value: Int = 0
)

data class SettingsUiState(
    // Profile
    val userName: String = "",
    val userEmail: String = "",
    val userRole: UserRole = UserRole.KASIR,
    val branchName: String = "",
    
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
    val lastSyncInfo: LastSyncInfo = LastSyncInfo(LastSyncUnit.NEVER),
    val pendingCount: Int = 0,
    val isSyncing: Boolean = false,
    
    // Tax
    val taxPercentage: Double = 0.5,
    
    // Performance
    val lowPerformanceMode: Boolean = false,
    
    // State
    val isLoggedOut: Boolean = false,
    val isLoading: Boolean = false,
    val isChangingPassword: Boolean = false,
    val passwordChangeError: PasswordChangeMessage? = null,
    val passwordChangeErrorDetail: String? = null,
    val passwordChangeSuccess: PasswordChangeMessage? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: Auth,
    private val appLockPreferences: AppLockPreferences,
    private val biometricManager: AppBiometricManager,
    private val syncManager: SyncManager,
    private val accessControlManager: AccessControlManager,
    private val sessionPreferences: SessionPreferences,
    private val appSettingsRepository: AppSettingsRepository,
    private val changeCurrentUserPasswordUseCase: ChangeCurrentUserPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeAccessControl()
        observeLockSettings()
        observePerformanceSettings()
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

                // Load tax percentage
                val taxPercentage = try { appSettingsRepository.getDefaultTaxPercentage() } catch (_: Exception) { 0.5 }

                // Load performance mode
                val lowPerformanceMode = try { appSettingsRepository.getSettings()?.lowPerformanceMode ?: false } catch (_: Exception) { false }

                _uiState.update { state ->
                    state.copy(
                        userName = userName,
                        userEmail = userEmail,
                        userRole = role,
                        isOwner = isOwner,
                        lockEnabled = lockEnabled,
                        biometricEnabled = biometricEnabled,
                        biometricAvailable = biometricAvailable,
                        pinSet = pinSet,
                        ownerPinSet = ownerPinSet,
                        lockableRoutes = lockableRoutes,
                        pendingCount = pendingCount,
                        lastSyncInfo = lastSyncInfo,
                        taxPercentage = taxPercentage,
                        lowPerformanceMode = lowPerformanceMode
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

    private fun observePerformanceSettings() {
        viewModelScope.launch {
            appSettingsRepository.observeSettings().collect { settings ->
                _uiState.update { it.copy(lowPerformanceMode = settings?.lowPerformanceMode ?: false) }
            }
        }
    }

    private fun extractUserName(user: UserInfo): String {
        return user.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User"
    }

    private fun formatLastSync(timestamp: Long): LastSyncInfo {
        if (timestamp == 0L) return LastSyncInfo(LastSyncUnit.NEVER)
        
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> LastSyncInfo(LastSyncUnit.JUST_NOW)
            diff < 3600_000 -> LastSyncInfo(LastSyncUnit.MINUTES_AGO, (diff / 60_000).toInt())
            diff < 86400_000 -> LastSyncInfo(LastSyncUnit.HOURS_AGO, (diff / 3600_000).toInt())
            else -> LastSyncInfo(LastSyncUnit.DAYS_AGO, (diff / 86400_000).toInt())
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

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        when {
            currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() -> {
                _uiState.update {
                    it.copy(
                        passwordChangeError = PasswordChangeMessage.REQUIRED_FIELDS,
                        passwordChangeErrorDetail = null,
                        passwordChangeSuccess = null
                    )
                }
                return
            }
            newPassword.length < 4 -> {
                _uiState.update {
                    it.copy(
                        passwordChangeError = PasswordChangeMessage.MIN_LENGTH,
                        passwordChangeErrorDetail = null,
                        passwordChangeSuccess = null
                    )
                }
                return
            }
            newPassword != confirmPassword -> {
                _uiState.update {
                    it.copy(
                        passwordChangeError = PasswordChangeMessage.CONFIRMATION_MISMATCH,
                        passwordChangeErrorDetail = null,
                        passwordChangeSuccess = null
                    )
                }
                return
            }
            currentPassword == newPassword -> {
                _uiState.update {
                    it.copy(
                        passwordChangeError = PasswordChangeMessage.SAME_AS_CURRENT,
                        passwordChangeErrorDetail = null,
                        passwordChangeSuccess = null
                    )
                }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isChangingPassword = true,
                    passwordChangeError = null,
                    passwordChangeErrorDetail = null,
                    passwordChangeSuccess = null
                )
            }

            when (val result = changeCurrentUserPasswordUseCase(currentPassword, newPassword)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isChangingPassword = false,
                            passwordChangeSuccess = PasswordChangeMessage.PASSWORD_CHANGED,
                            passwordChangeError = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isChangingPassword = false,
                            passwordChangeError = PasswordChangeMessage.GENERIC_FAILURE,
                            passwordChangeErrorDetail = result.exception?.message,
                            passwordChangeSuccess = null
                        )
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isChangingPassword = true) }
                }
            }
        }
    }

    fun clearPasswordChangeMessage() {
        _uiState.update {
            it.copy(
                passwordChangeError = null,
                passwordChangeErrorDetail = null,
                passwordChangeSuccess = null
            )
        }
    }

    fun updateTaxPercentage(percentage: Double) {
        viewModelScope.launch {
            try {
                appSettingsRepository.updateDefaultTaxPercentage(percentage)
                _uiState.update { it.copy(taxPercentage = percentage) }
                Timber.d("Tax percentage updated to $percentage%")
            } catch (e: Exception) {
                Timber.e(e, "Failed to update tax percentage")
            }
        }
    }
    fun updateLowPerformanceMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                appSettingsRepository.updateLowPerformanceMode(enabled)
                _uiState.update { it.copy(lowPerformanceMode = enabled) }
                Timber.d("Low performance mode updated to $enabled")
            } catch (e: Exception) {
                Timber.e(e, "Failed to update low performance mode")
            }
        }
    }
}
