package com.zatiaras.pos.core.domain.usecase

import com.zatiaras.pos.core.domain.AuthRepository
import com.zatiaras.pos.core.domain.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChangeCurrentUserPasswordUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var changeCurrentUserPasswordUseCase: ChangeCurrentUserPasswordUseCase

    @Before
    fun setup() {
        authRepository = mockk()
        changeCurrentUserPasswordUseCase = ChangeCurrentUserPasswordUseCase(authRepository)
    }

    @Test
    fun `invoke delegates to repository and returns Success`() = runTest {
        coEvery {
            authRepository.changeCurrentUserPassword("old-password", "new-password")
        } returns Result.Success(Unit)

        val result = changeCurrentUserPasswordUseCase("old-password", "new-password")

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) {
            authRepository.changeCurrentUserPassword("old-password", "new-password")
        }
    }

    @Test
    fun `invoke propagates Error from repository`() = runTest {
        val expectedMessage = "Password saat ini salah"
        coEvery {
            authRepository.changeCurrentUserPassword(any(), any())
        } returns Result.Error(Exception(expectedMessage))

        val result = changeCurrentUserPasswordUseCase("wrong-old", "new-password")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception?.message == expectedMessage)
    }
}
