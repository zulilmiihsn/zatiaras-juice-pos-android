package com.zatiaras.pos.core.data.access

import com.zatiaras.pos.core.data.local.dao.AppSettingsDao
import com.zatiaras.pos.core.data.local.entity.AppSettingsEntity
import com.zatiaras.pos.core.data.repository.AppSettingsRepository
import com.zatiaras.pos.core.data.session.SessionPreferences
import com.zatiaras.pos.core.domain.access.AccessCheckResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccessControlManagerTest {

    private lateinit var sessionPreferences: SessionPreferences
    private lateinit var accessControlPreferences: AccessControlPreferences
    private lateinit var appSettingsRepository: AppSettingsRepository
    private lateinit var appSettingsDao: AppSettingsDao
    private lateinit var manager: AccessControlManager

    @Before
    fun setup() {
        sessionPreferences = mockk(relaxed = true)
        accessControlPreferences = mockk(relaxed = true)
        appSettingsRepository = mockk(relaxed = true)
        appSettingsDao = mockk(relaxed = true)

        manager = AccessControlManager(
            sessionPreferences = sessionPreferences,
            accessControlPreferences = accessControlPreferences,
            appSettingsRepository = appSettingsRepository,
            appSettingsDao = appSettingsDao,
        )
    }

    @Test
    fun `verifyOwnerPin returns false when lockout is active`() = runTest {
        coEvery { accessControlPreferences.getOwnerPinLockoutRemainingMillis(any()) } returns 5_000L

        val result = manager.verifyOwnerPin("1234")

        assertFalse(result)
        coVerify(exactly = 0) { appSettingsRepository.verifyOwnerPin(any()) }
        coVerify(exactly = 0) { accessControlPreferences.verifyOwnerPin(any()) }
    }

    @Test
    fun `verifyOwnerPin clears lockout on successful verification`() = runTest {
        coEvery { accessControlPreferences.getOwnerPinLockoutRemainingMillis(any()) } returns 0L
        coEvery { appSettingsRepository.verifyOwnerPin("1234") } returns true

        val result = manager.verifyOwnerPin("1234")

        assertTrue(result)
        coVerify(exactly = 1) { accessControlPreferences.clearOwnerPinLockout() }
        coVerify(exactly = 0) { accessControlPreferences.recordFailedOwnerPinAttempt(any()) }
    }

    @Test
    fun `verifyOwnerPin records failed attempt when synced verification fails`() = runTest {
        coEvery { accessControlPreferences.getOwnerPinLockoutRemainingMillis(any()) } returns 0L
        coEvery { appSettingsRepository.verifyOwnerPin("0000") } throws RuntimeException("offline")
        coEvery { accessControlPreferences.verifyOwnerPin("0000") } returns false

        val result = manager.verifyOwnerPin("0000")

        assertFalse(result)
        coVerify(exactly = 1) { accessControlPreferences.recordFailedOwnerPinAttempt(any()) }
    }

    @Test
    fun `checkAccess denies cashier when locked route has no owner pin`() = runTest {
        every { sessionPreferences.getRole() } returns UserRole.KASIR.value
        every {
            appSettingsDao.observeSettings()
        } returns flowOf(
            AppSettingsEntity(
                lockedRoutes = listOf(LockableRoute.PNL_REPORT.route),
                ownerPinHash = null,
            ),
        )

        val result = manager.checkAccessNow(LockableRoute.PNL_REPORT.route)

        assertEquals(AccessCheckResult.DeniedOwnerPinNotSet, result)
    }

    @Test
    fun `checkAccess requires pin for cashier when locked route has owner pin`() = runTest {
        every { sessionPreferences.getRole() } returns UserRole.KASIR.value
        every {
            appSettingsDao.observeSettings()
        } returns flowOf(
            AppSettingsEntity(
                lockedRoutes = listOf(LockableRoute.PNL_REPORT.route),
                ownerPinHash = "hash",
            ),
        )

        val result = manager.checkAccessNow(LockableRoute.PNL_REPORT.route)

        assertEquals(AccessCheckResult.RequiresOwnerPin, result)
    }

    @Test
    fun `checkAccess grants owner even when route is locked and pin is not set`() = runTest {
        every { sessionPreferences.getRole() } returns UserRole.PEMILIK.value
        every {
            appSettingsDao.observeSettings()
        } returns flowOf(
            AppSettingsEntity(
                lockedRoutes = listOf(LockableRoute.PNL_REPORT.route),
                ownerPinHash = null,
            ),
        )

        val result = manager.checkAccessNow(LockableRoute.PNL_REPORT.route)

        assertEquals(AccessCheckResult.Granted, result)
    }
}
