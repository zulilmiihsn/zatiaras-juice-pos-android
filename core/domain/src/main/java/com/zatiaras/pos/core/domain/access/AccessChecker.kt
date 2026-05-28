package com.zatiaras.pos.core.domain.access

import kotlinx.coroutines.flow.Flow

sealed class AccessCheckResult {
    data object Granted : AccessCheckResult()
    data object RequiresOwnerPin : AccessCheckResult()
    data object DeniedOwnerPinNotSet : AccessCheckResult()
}

interface AccessChecker {
    fun checkAccess(route: String): Flow<AccessCheckResult>
    suspend fun checkAccessNow(route: String): AccessCheckResult
    suspend fun verifyOwnerPin(pin: String): Boolean
}
