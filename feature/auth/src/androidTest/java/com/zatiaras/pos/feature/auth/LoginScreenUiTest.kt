package com.zatiaras.pos.feature.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zatiaras.pos.core.ui.theme.ZatiarasPOSTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for Login screen.
 * 
 * Tests:
 * - Login form display
 * - Username/password input
 * - Login button state
 * - Error message display
 * - Loading state
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val defaultState = AuthUiState()

    @Test
    fun loginScreen_displaysUsernameField() {
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                LoginScreenContent(
                    uiState = defaultState,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {},
                    onPasswordVisibilityToggle = {}
                )
            }
        }

        // Verify username field exists
        composeTestRule.onNodeWithText("Username", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun loginScreen_displaysPasswordField() {
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                LoginScreenContent(
                    uiState = defaultState,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {},
                    onPasswordVisibilityToggle = {}
                )
            }
        }

        // Verify password field exists
        composeTestRule.onNodeWithText("Password", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun loginScreen_displaysLoginButton() {
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                LoginScreenContent(
                    uiState = defaultState,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {},
                    onPasswordVisibilityToggle = {}
                )
            }
        }

        // Verify login button exists
        composeTestRule.onNodeWithText("Masuk", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun loginScreen_emptyFields_loginButtonDisabled() {
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                LoginScreenContent(
                    uiState = defaultState,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {},
                    onPasswordVisibilityToggle = {}
                )
            }
        }

        // Verify login button is disabled when fields are empty
        composeTestRule.onNodeWithText("Masuk", substring = true, ignoreCase = true)
            .assertExists()
            .assertIsNotEnabled()
    }

    @Test
    fun loginScreen_filledFields_loginButtonEnabled() {
        val filledState = defaultState.copy(
            username = "admin",
            password = "password123"
        )
        
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                LoginScreenContent(
                    uiState = filledState,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {},
                    onPasswordVisibilityToggle = {}
                )
            }
        }

        // Verify login button is enabled when fields are filled
        composeTestRule.onNodeWithText("Masuk", substring = true, ignoreCase = true)
            .assertExists()
            .assertIsEnabled()
    }

    @Test
    fun loginScreen_loading_showsProgressIndicator() {
        val loadingState = defaultState.copy(
            isLoading = true
        )
        
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                LoginScreenContent(
                    uiState = loadingState,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {},
                    onPasswordVisibilityToggle = {}
                )
            }
        }

        // Verify progress indicator is shown
        composeTestRule.onNodeWithTag("loading_indicator")
            .assertExists()
    }

    @Test
    fun loginScreen_error_showsErrorMessage() {
        val errorState = defaultState.copy(
            error = "Username tidak ditemukan"
        )
        
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                LoginScreenContent(
                    uiState = errorState,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {},
                    onPasswordVisibilityToggle = {}
                )
            }
        }

        // Verify error message is displayed
        composeTestRule.onNodeWithText("Username tidak ditemukan", substring = true)
            .assertExists()
    }

    @Test
    fun loginScreen_typeUsername_callsOnUsernameChange() {
        var usernameValue = ""
        
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                LoginScreenContent(
                    uiState = defaultState,
                    onUsernameChange = { usernameValue = it },
                    onPasswordChange = {},
                    onLoginClick = {},
                    onPasswordVisibilityToggle = {}
                )
            }
        }

        // Type in username field
        composeTestRule.onNodeWithText("Username", substring = true, ignoreCase = true)
            .performTextInput("testuser")

        assert(usernameValue == "testuser") { "Username should be 'testuser'" }
    }

    @Test
    fun loginScreen_typePassword_callsOnPasswordChange() {
        var passwordValue = ""
        
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                LoginScreenContent(
                    uiState = defaultState,
                    onUsernameChange = {},
                    onPasswordChange = { passwordValue = it },
                    onLoginClick = {},
                    onPasswordVisibilityToggle = {}
                )
            }
        }

        // Type in password field
        composeTestRule.onNodeWithText("Password", substring = true, ignoreCase = true)
            .performTextInput("secret123")

        assert(passwordValue == "secret123") { "Password should be 'secret123'" }
    }

    @Test
    fun loginScreen_clickLogin_callsOnLoginClick() {
        var loginClicked = false
        
        val filledState = defaultState.copy(
            username = "admin",
            password = "password"
        )
        
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                LoginScreenContent(
                    uiState = filledState,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = { loginClicked = true },
                    onPasswordVisibilityToggle = {}
                )
            }
        }

        // Click login button
        composeTestRule.onNodeWithText("Masuk", substring = true, ignoreCase = true)
            .performClick()

        assert(loginClicked) { "onLoginClick should be called" }
    }

    @Test
    fun loginScreen_passwordVisibilityToggle_exists() {
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                LoginScreenContent(
                    uiState = defaultState,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {},
                    onPasswordVisibilityToggle = {}
                )
            }
        }

        // Verify visibility toggle icon exists
        composeTestRule.onNodeWithContentDescription("Toggle password visibility", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun loginScreen_displaysAppBranding() {
        composeTestRule.setContent {
            ZatiarasPOSTheme {
                LoginScreenContent(
                    uiState = defaultState,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {},
                    onPasswordVisibilityToggle = {}
                )
            }
        }

        // Verify app name/branding is displayed
        composeTestRule.onNodeWithText("ZATIARAS", substring = true, ignoreCase = true)
            .assertExists()
    }
}
