package com.zatiaras.pos.core.domain.usecase

import com.zatiaras.pos.core.domain.AuthRepository
import com.zatiaras.pos.core.domain.Result
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(username: String, password: String): Result<Unit> {
        if (username.isBlank()) {
            return Result.Error(IllegalArgumentException("Username tidak boleh kosong"))
        }
        if (password.isBlank()) {
            return Result.Error(IllegalArgumentException("Password tidak boleh kosong"))
        }
        return authRepository.login(username, password)
    }
}
