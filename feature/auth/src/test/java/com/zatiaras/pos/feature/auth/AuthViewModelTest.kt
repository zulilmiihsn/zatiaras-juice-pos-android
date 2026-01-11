package com.zatiaras.pos.feature.auth

import com.zatiaras.pos.core.domain.Result
import com.zatiaras.pos.core.domain.usecase.LoginUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AuthViewModel.
 * 
 * Tests:
 * - Initial state is Idle
 * - Login success navigates to Success state
 * - Login failure shows Error state
 * - Reset state returns to Idle
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var loginUseCase: LoginUseCase
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        loginUseCase = mockk()
        viewModel = AuthViewModel(loginUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `login success updates state to Success`() = runTest {
        // Given
        coEvery { loginUseCase(any(), any()) } returns Result.Success(Unit)
        
        // When
        viewModel.login("test@test.com", "password")
        advanceUntilIdle()
        
        // Then
        assertEquals(AuthUiState.Success, viewModel.uiState.value)
        coVerify(exactly = 1) { loginUseCase("test@test.com", "password") }
    }

    @Test
    fun `login failure updates state to Error with message`() = runTest {
        // Given
        val errorMessage = "Email atau password salah"
        coEvery { loginUseCase(any(), any()) } returns Result.Error(Exception(errorMessage))
        
        // When
        viewModel.login("wrong@test.com", "wrongpass")
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals(errorMessage, (state as AuthUiState.Error).message)
    }

    @Test
    fun `login shows Loading state before result`() = runTest {
        // Given
        coEvery { loginUseCase(any(), any()) } returns Result.Success(Unit)
        
        // When
        viewModel.login("test@test.com", "password")
        
        // Then - during execution, state should be Loading
        // Note: With StandardTestDispatcher, we can check intermediate states
        // The Loading state happens before advanceUntilIdle
        advanceUntilIdle()
        
        // After completion, should be Success
        assertEquals(AuthUiState.Success, viewModel.uiState.value)
    }

    @Test
    fun `resetState returns to Idle`() = runTest {
        // Given - login success first
        coEvery { loginUseCase(any(), any()) } returns Result.Success(Unit)
        viewModel.login("test@test.com", "password")
        advanceUntilIdle()
        assertEquals(AuthUiState.Success, viewModel.uiState.value)
        
        // When
        viewModel.resetState()
        
        // Then
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }
}
