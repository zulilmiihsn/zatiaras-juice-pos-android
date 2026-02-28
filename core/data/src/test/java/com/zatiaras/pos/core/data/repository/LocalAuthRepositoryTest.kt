package com.zatiaras.pos.core.data.repository

import com.zatiaras.pos.core.data.local.dao.UserDao
import com.zatiaras.pos.core.data.local.entity.UserEntity
import com.zatiaras.pos.core.data.remote.UserRemoteDataSource
import com.zatiaras.pos.core.data.remote.dto.UserDto
import com.zatiaras.pos.core.data.session.SessionPreferences
import com.zatiaras.pos.core.domain.Result
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LocalAuthRepository.
 * 
 * Tests:
 * - Login with valid credentials
 * - Login with invalid credentials
 * - Login with inactive user
 * - Logout clears session
 * - Session restoration
 * - User sync from remote
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalAuthRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var userDao: UserDao
    private lateinit var userRemoteDataSource: UserRemoteDataSource
    private lateinit var sessionPreferences: SessionPreferences
    private lateinit var repository: LocalAuthRepository

    private val testUser = UserEntity(
        id = "user-1",
        username = "admin",
        passwordHash = UserEntity.hashPassword("admin123"),
        displayName = "Administrator",
        role = "pemilik",
        isActive = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        userDao = mockk(relaxed = true)
        userRemoteDataSource = mockk(relaxed = true)
        sessionPreferences = mockk(relaxed = true)
        
        repository = LocalAuthRepository(userDao, userRemoteDataSource, sessionPreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Login Tests ====================

    @Test
    fun `login with valid credentials returns Success`() = runTest {
        coEvery { userDao.getUserByUsername("admin") } returns testUser
        
        val result = repository.login("admin", "admin123")
        
        assertTrue(result is Result.Success)
    }

    @Test
    fun `login with valid credentials saves session`() = runTest {
        coEvery { userDao.getUserByUsername("admin") } returns testUser
        
        repository.login("admin", "admin123")
        
        coVerify { 
            sessionPreferences.saveSession(
                userId = testUser.id,
                username = testUser.username,
                displayName = testUser.displayName,
                role = testUser.role
            ) 
        }
    }

    @Test
    fun `login with non-existent user returns Error`() = runTest {
        coEvery { userDao.getUserByUsername("unknown") } returns null
        
        val result = repository.login("unknown", "password")
        
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception?.message?.contains("tidak ditemukan") == true)
    }

    @Test
    fun `login with wrong password returns Error`() = runTest {
        coEvery { userDao.getUserByUsername("admin") } returns testUser
        
        val result = repository.login("admin", "wrongpassword")
        
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception?.message?.contains("Password salah") == true)
    }

    @Test
    fun `login with inactive user returns Error`() = runTest {
        val inactiveUser = testUser.copy(isActive = false)
        coEvery { userDao.getUserByUsername("admin") } returns inactiveUser
        
        val result = repository.login("admin", "admin123")
        
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception?.message?.contains("tidak aktif") == true)
    }

    // ==================== Logout Tests ====================

    @Test
    fun `logout clears session`() = runTest {
        // First login
        coEvery { userDao.getUserByUsername("admin") } returns testUser
        repository.login("admin", "admin123")
        
        // Then logout
        repository.logout()
        
        coVerify { sessionPreferences.clearSession() }
    }

    @Test
    fun `logout clears current user`() = runTest {
        coEvery { userDao.getUserByUsername("admin") } returns testUser
        repository.login("admin", "admin123")
        
        assertEquals(testUser, repository.getCurrentUser())
        
        repository.logout()
        
        assertNull(repository.getCurrentUser())
    }

    // ==================== Session Restoration Tests ====================

    @Test
    fun `restoreSession returns true when valid session exists`() = runTest {
        every { sessionPreferences.isLoggedIn() } returns true
        every { sessionPreferences.getUserId() } returns testUser.id
        coEvery { userDao.getUserById(testUser.id) } returns testUser
        
        val result = repository.restoreSession()
        
        assertTrue(result)
        assertEquals(testUser, repository.getCurrentUser())
    }

    @Test
    fun `restoreSession returns false when no session exists`() = runTest {
        every { sessionPreferences.isLoggedIn() } returns false
        
        val result = repository.restoreSession()
        
        assertFalse(result)
    }

    @Test
    fun `restoreSession returns false when user not found`() = runTest {
        every { sessionPreferences.isLoggedIn() } returns true
        every { sessionPreferences.getUserId() } returns testUser.id
        coEvery { userDao.getUserById(testUser.id) } returns null
        
        val result = repository.restoreSession()
        
        assertFalse(result)
        coVerify { sessionPreferences.clearSession() }
    }

    @Test
    fun `restoreSession returns false when user is inactive`() = runTest {
        val inactiveUser = testUser.copy(isActive = false)
        every { sessionPreferences.isLoggedIn() } returns true
        every { sessionPreferences.getUserId() } returns testUser.id
        coEvery { userDao.getUserById(testUser.id) } returns inactiveUser
        
        val result = repository.restoreSession()
        
        assertFalse(result)
        coVerify { sessionPreferences.clearSession() }
    }

    // ==================== hasSavedSession Tests ====================

    @Test
    fun `hasSavedSession returns true when logged in`() {
        every { sessionPreferences.isLoggedIn() } returns true
        
        assertTrue(repository.hasSavedSession())
    }

    @Test
    fun `hasSavedSession returns false when not logged in`() {
        every { sessionPreferences.isLoggedIn() } returns false
        
        assertFalse(repository.hasSavedSession())
    }

    // ==================== First Run Tests ====================

    @Test
    fun `isFirstRun returns true when no users exist`() = runTest {
        coEvery { userDao.getUserCount() } returns 0
        
        assertTrue(repository.isFirstRun())
    }

    @Test
    fun `isFirstRun returns false when users exist`() = runTest {
        coEvery { userDao.getUserCount() } returns 5
        
        assertFalse(repository.isFirstRun())
    }

    // ==================== Sync Tests ====================

    @Test
    fun `syncUsersFromRemote returns count of synced users`() = runTest {
        val remoteUsers = listOf(
            UserDto(
                id = "user-1", 
                username = "admin", 
                passwordHash = "hash1",
                displayName = "Admin",
                role = "pemilik",
                isActive = true
            ),
            UserDto(
                id = "user-2", 
                username = "kasir1", 
                passwordHash = "hash2",
                displayName = "Kasir 1",
                role = "kasir",
                isActive = true
            )
        )
        coEvery { userRemoteDataSource.fetchActiveUsers() } returns Result.Success(remoteUsers)
        coEvery { userDao.getUserByUsername(any()) } returns null
        
        val count = repository.syncUsersFromRemote()
        
        assertEquals(2, count)
        coVerify(exactly = 2) { userDao.insertUser(any()) }
    }

    @Test
    fun `syncUsersFromRemote returns -1 on failure`() = runTest {
        coEvery { userRemoteDataSource.fetchActiveUsers() } returns Result.Error(Exception("Network error"))
        
        val count = repository.syncUsersFromRemote()
        
        assertEquals(-1, count)
    }

    // ==================== getAllUsers Tests ====================

    @Test
    fun `getAllUsers returns list from DAO`() = runTest {
        val users = listOf(testUser, testUser.copy(id = "user-2", username = "kasir"))
        coEvery { userDao.getAllUsersList() } returns users
        
        val result = repository.getAllUsers()
        
        assertEquals(2, result.size)
    }

    // ==================== Change Password Tests ====================

    @Test
    fun `changeCurrentUserPassword with valid current password returns Success`() = runTest {
        coEvery { userDao.getUserByUsername("admin") } returns testUser
        every { sessionPreferences.getUserId() } returns testUser.id
        coEvery { userRemoteDataSource.updatePasswordHash(eq(testUser.id), any()) } returns Result.Success(Unit)

        repository.login("admin", "admin123")

        val result = repository.changeCurrentUserPassword("admin123", "newpass123")

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { userDao.updatePassword(eq(testUser.id), any(), any()) }
        coVerify(exactly = 1) { userRemoteDataSource.updatePasswordHash(eq(testUser.id), any()) }
    }

    @Test
    fun `changeCurrentUserPassword with wrong current password returns Error`() = runTest {
        coEvery { userDao.getUserByUsername("admin") } returns testUser
        every { sessionPreferences.getUserId() } returns testUser.id

        repository.login("admin", "admin123")

        val result = repository.changeCurrentUserPassword("wrongpass", "newpass123")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception?.message?.contains("saat ini salah") == true)
        coVerify(exactly = 0) { userDao.updatePassword(any(), any(), any()) }
        coVerify(exactly = 0) { userRemoteDataSource.updatePasswordHash(any(), any()) }
    }

    @Test
    fun `changeCurrentUserPassword fails when remote update fails`() = runTest {
        coEvery { userDao.getUserByUsername("admin") } returns testUser
        every { sessionPreferences.getUserId() } returns testUser.id
        coEvery { userRemoteDataSource.updatePasswordHash(eq(testUser.id), any()) } returns Result.Error(Exception("Remote gagal"))

        repository.login("admin", "admin123")

        val result = repository.changeCurrentUserPassword("admin123", "newpass123")

        assertTrue(result is Result.Error)
        coVerify(exactly = 0) { userDao.updatePassword(any(), any(), any()) }
        coVerify(exactly = 1) { userRemoteDataSource.updatePasswordHash(eq(testUser.id), any()) }
    }
}
