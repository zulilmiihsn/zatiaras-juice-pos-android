package com.zatiaras.pos.core.domain.usecase

import com.zatiaras.pos.core.domain.AuthRepository
import com.zatiaras.pos.core.domain.Result
import javax.inject.Inject

class ChangeCurrentUserPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(currentPassword: String, newPassword: String): Result<Unit> {
        return authRepository.changeCurrentUserPassword(currentPassword, newPassword)
    }
}
