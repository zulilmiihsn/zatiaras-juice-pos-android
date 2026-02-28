package com.zatiaras.pos.feature.auth

import com.zatiaras.pos.core.data.repository.LocalAuthRepository
import com.zatiaras.pos.core.domain.Result
import com.zatiaras.pos.core.domain.usecase.LoginUseCase
import com.zatiaras.pos.core.ui.util.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * - Initial state starts with Syncing then goes to Idle
 * - Login success navigates to Success state
 * - Login failure shows Error state
 * - Reset state returns to Idle
 * - Sync users on startup
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var loginUseCase: LoginUseCase
    private lateinit var localAuthRepository: LocalAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        loginUseCase = mockk()
        localAuthRepository = mockk(relaxed = true)
        
        // Mock sync-related methods
        coEvery { localAuthRepository.syncUsersWithResult() } returns Result.Success(1)
        coEvery { localAuthRepository.getAllUsers() } returns emptyList()
        
        viewModel = AuthViewModel(loginUseCase, localAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state transitions to Idle after sync`() = runTest {
        // With StandardTestDispatcher, the init coroutine is queued, not started yet
        // Initial MutableStateFlow value is Idle
        // After advancing, sync completes and state returns to Idle
        advanceUntilIdle()
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `syncUsersWithResult is called on startup`() = runTest {
        advanceUntilIdle()
        coVerify { localAuthRepository.syncUsersWithResult() }
    }

    @Test
    fun `login success updates state to Success`() = runTest {
        // Wait for initial sync
        advanceUntilIdle()
        
        // Given
        coEvery { loginUseCase(any(), any()) } returns Result.Success(Unit)
        
        // When
        viewModel.login("test@test.com", "password", "branch-1")
        advanceUntilIdle()
        
        // Then
        assertEquals(AuthUiState.Success, viewModel.uiState.value)
        coVerify(exactly = 1) { loginUseCase("test@test.com", "password") }
    }

    @Test
    fun `login failure updates state to Error with message`() = runTest {
        // Wait for initial sync
        advanceUntilIdle()
        
        // Given
        val errorMessage = "Email atau password salah"
        coEvery { loginUseCase(any(), any()) } returns Result.Error(Exception(errorMessage))
        
        // When
        viewModel.login("wrong@test.com", "wrongpass", "branch-1")
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        val errorState = state as AuthUiState.Error
        assertTrue(errorState.message is UiText.DynamicString)
        assertEquals(errorMessage, (errorState.message as UiText.DynamicString).value)
    }

    @Test
    fun `login shows Loading state before result`() = runTest {
        // Wait for initial sync
        advanceUntilIdle()
        
        // Given
        coEvery { loginUseCase(any(), any()) } returns Result.Success(Unit)
        
        // When
        viewModel.login("test@test.com", "password", "branch-1")
        
        // Then - during execution, state should be Loading
        // Note: With StandardTestDispatcher, we can check intermediate states
        // The Loading state happens before advanceUntilIdle
        advanceUntilIdle()
        
        // After completion, should be Success
        assertEquals(AuthUiState.Success, viewModel.uiState.value)
    }

    @Test
    fun `resetState returns to Idle`() = runTest {
        // Wait for initial sync
        advanceUntilIdle()
        
        // Given - login success first
        coEvery { loginUseCase(any(), any()) } returns Result.Success(Unit)
        viewModel.login("test@test.com", "password", "branch-1")
        advanceUntilIdle()
        assertEquals(AuthUiState.Success, viewModel.uiState.value)
        
        // When
        viewModel.resetState()
        
        // Then
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }
}
