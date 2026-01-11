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

/**
 * Unit tests for LoginUseCase.
 * 
 * Tests:
 * - Successful login returns Result.Success
 * - Failed login returns Result.Error with message
 * - Empty email/password validation
 */
class LoginUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var loginUseCase: LoginUseCase

    @Before
    fun setup() {
        authRepository = mockk()
        loginUseCase = LoginUseCase(authRepository)
    }

    @Test
    fun `login with valid credentials returns Success`() = runTest {
        // Given
        coEvery { authRepository.login(any(), any()) } returns Result.Success(Unit)
        
        // When
        val result = loginUseCase("test@zatiaras.com", "password123")
        
        // Then
        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { authRepository.login("test@zatiaras.com", "password123") }
    }

    @Test
    fun `login with invalid credentials returns Error`() = runTest {
        // Given
        val errorMessage = "Email atau password salah"
        coEvery { authRepository.login(any(), any()) } returns Result.Error(Exception(errorMessage))
        
        // When
        val result = loginUseCase("wrong@email.com", "wrongpassword")
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception?.message == errorMessage)
    }

    @Test
    fun `login with empty email returns Error`() = runTest {
        // When
        val result = loginUseCase("", "password123")
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception?.message?.contains("empty", ignoreCase = true) == true)
    }

    @Test
    fun `login with empty password returns Error`() = runTest {
        // When
        val result = loginUseCase("test@zatiaras.com", "")
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception?.message?.contains("empty", ignoreCase = true) == true)
    }
}
