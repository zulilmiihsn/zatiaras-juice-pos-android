package com.zatiaras.pos.feature.auth.lock

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of biometric authentication attempt.
 */
sealed class BiometricResult {
    data object Success : BiometricResult()
    data object Cancelled : BiometricResult()
    data class Error(val code: Int, val message: String) : BiometricResult()
    data object NotAvailable : BiometricResult()
    data object NotEnrolled : BiometricResult()
}

/**
 * Availability status of biometric authentication.
 */
enum class BiometricAvailability {
    AVAILABLE,
    NOT_AVAILABLE,
    NOT_ENROLLED,
    HARDWARE_UNAVAILABLE
}

/**
 * Manages biometric authentication for app lock.
 * 
 * Supports:
 * - Fingerprint
 * - Face recognition
 * - Device credential fallback (PIN/Pattern/Password)
 */
@Singleton
class AppBiometricManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val biometricManager = BiometricManager.from(context)

    /**
     * Check if biometric authentication is available.
     */
    fun checkAvailability(): BiometricAvailability {
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NOT_AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            else -> BiometricAvailability.NOT_AVAILABLE
        }
    }

    /**
     * Check if biometric is available and enrolled.
     */
    fun isBiometricAvailable(): Boolean {
        return checkAvailability() == BiometricAvailability.AVAILABLE
    }

    /**
     * Check if device credential (PIN/Pattern/Password) is available.
     */
    fun isDeviceCredentialAvailable(): Boolean {
        return biometricManager.canAuthenticate(DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Show biometric prompt for authentication.
     * 
     * @param activity The activity to show the prompt on
     * @param title Title text for the prompt
     * @param subtitle Subtitle text for the prompt
     * @param negativeButtonText Text for the cancel button
     * @param onResult Callback with the result
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Verifikasi Identitas",
        subtitle: String = "Gunakan sidik jari atau wajah untuk membuka aplikasi",
        negativeButtonText: String = "Gunakan PIN",
        onResult: (BiometricResult) -> Unit
    ) {
        if (!isBiometricAvailable()) {
            Timber.w("Biometric not available")
            onResult(BiometricResult.NotAvailable)
            return
        }

        val executor = ContextCompat.getMainExecutor(context)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Timber.d("Biometric authentication succeeded")
                onResult(BiometricResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Timber.w("Biometric authentication error: $errorCode - $errString")
                
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        onResult(BiometricResult.Cancelled)
                    }
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                        onResult(BiometricResult.NotEnrolled)
                    }
                    else -> {
                        onResult(BiometricResult.Error(errorCode, errString.toString()))
                    }
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Timber.w("Biometric authentication failed (not recognized)")
                // Don't call onResult here - user can retry
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Show biometric prompt with device credential fallback.
     */
    fun authenticateWithDeviceCredential(
        activity: FragmentActivity,
        title: String = "Verifikasi Identitas",
        subtitle: String = "Gunakan sidik jari, wajah, atau PIN perangkat",
        onResult: (BiometricResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Timber.d("Authentication succeeded")
                onResult(BiometricResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Timber.w("Authentication error: $errorCode - $errString")
                
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED -> {
                        onResult(BiometricResult.Cancelled)
                    }
                    else -> {
                        onResult(BiometricResult.Error(errorCode, errString.toString()))
                    }
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Timber.w("Authentication failed (not recognized)")
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }
}
