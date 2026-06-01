package com.shopsafe.tests;

import com.shopsafe.pages.InventoryPage;
import com.shopsafe.pages.LoginPage;
import com.shopsafe.utils.ConfigReader;
import com.shopsafe.utils.TestDataReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoginTest — 9 test cases covering the Sauce Demo login module.
 *
 * Python equivalent: tests/test_login.py
 *
 * JUnit 5 annotations used:
 *  @Test            — marks a test method (equivalent to pytest's auto-discovery by name prefix)
 *  @DisplayName     — human-readable name in test reports (equivalent to pytest's test docstring)
 *  @BeforeEach      — runs before every test in this class (sets up a fresh LoginPage)
 *  @ParameterizedTest + @MethodSource — data-driven tests from a Stream<Arguments>
 *                   (equivalent to @pytest.mark.parametrize)
 *
 * Zero Selenium in this file — all driver interactions go through LoginPage.
 * Tests call page object methods and then make assertions. Nothing else.
 */
@DisplayName("Login Module")
class LoginTest extends BaseTest {

    private LoginPage loginPage;

    /**
     * Open the login page before each test.
     *
     * Python equivalent: the "driver" fixture navigating to base_url,
     *   and LoginPage(driver) being constructed at the top of each test.
     */
    @BeforeEach
    void openLoginPage() {
        loginPage = new LoginPage(driver);
        loginPage.open(baseUrl);
    }

    // ── Positive tests ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid credentials → redirects to inventory page")
    void testValidLogin() {
        InventoryPage inventoryPage = loginPage.login(
            ConfigReader.getStandardUsername(),
            ConfigReader.getPassword()
        );
        // Assert we landed on the inventory page.
        // getCurrentUrl() is called on the driver inside BasePage.
        assertTrue(driver.getCurrentUrl().contains("inventory"),
                   "URL should contain 'inventory' after successful login");
    }

    @Test
    @DisplayName("Valid login → inventory page title is 'Products'")
    void testInventoryTitleAfterLogin() {
        InventoryPage inventoryPage = loginPage.login(
            ConfigReader.getStandardUsername(),
            ConfigReader.getPassword()
        );
        assertEquals("Products", inventoryPage.getPageTitle(),
                     "Page title should be 'Products' after login");
    }

    // ── Negative tests ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Locked-out user → shows error banner")
    void testLockedOutUser() {
        loginPage.attemptLogin(ConfigReader.getLockedUsername(), ConfigReader.getPassword());
        assertTrue(loginPage.isErrorDisplayed(), "Error banner should be visible");
        assertTrue(loginPage.getErrorMessage().contains("locked out"),
                   "Error should mention 'locked out'");
    }

    @Test
    @DisplayName("Wrong password → shows error banner")
    void testWrongPassword() {
        loginPage.attemptLogin(ConfigReader.getStandardUsername(), "wrong_password");
        assertTrue(loginPage.isErrorDisplayed(), "Error banner should be visible");
        assertTrue(loginPage.getErrorMessage().contains("Username and password do not match"),
                   "Error should mention credentials mismatch");
    }

    @Test
    @DisplayName("Empty username → error: 'Username is required'")
    void testEmptyUsername() {
        loginPage.attemptLogin("", ConfigReader.getPassword());
        assertEquals("Epic sadface: Username is required", loginPage.getErrorMessage());
    }

    @Test
    @DisplayName("Empty password → error: 'Password is required'")
    void testEmptyPassword() {
        loginPage.attemptLogin(ConfigReader.getStandardUsername(), "");
        assertEquals("Epic sadface: Password is required", loginPage.getErrorMessage());
    }

    @Test
    @DisplayName("Both fields empty → error: 'Username is required'")
    void testBothFieldsEmpty() {
        loginPage.attemptLogin("", "");
        assertEquals("Epic sadface: Username is required", loginPage.getErrorMessage());
    }

    @Test
    @DisplayName("Error banner is dismissed when × button is clicked")
    void testErrorDismissedOnClick() {
        // Trigger an error first.
        loginPage.attemptLogin("", "");
        assertTrue(loginPage.isErrorDisplayed(), "Error should be visible before dismissal");

        // Dismiss it.
        loginPage.dismissError();

        // Assert it's gone.
        assertFalse(loginPage.isErrorDisplayed(), "Error should not be visible after dismissal");
    }

    // ── Data-driven test ──────────────────────────────────────────────────────

    /**
     * Data-driven valid login — reads credentials from login_data.json.
     *
     * Python equivalent: @pytest.mark.parametrize via data_reader.py loading login_data.json
     *
     * @ParameterizedTest tells JUnit to run this method once per item in the stream.
     * @MethodSource("loginDataProvider") points to the static method below.
     *
     * Each LoginData item contains { username, password } — Jackson deserialized them
     * from JSON in TestDataReader.
     */
    @ParameterizedTest(name = "Data-driven login: {0}")
    @MethodSource("loginDataProvider")
    @DisplayName("Data-driven: valid login from JSON")
    void testDataDrivenValidLogin(TestDataReader.LoginData data) {
        InventoryPage inventoryPage = loginPage.login(data.username, data.password);
        assertTrue(driver.getCurrentUrl().contains("inventory"),
                   "Should reach inventory after data-driven login with: " + data.username);
    }

    /**
     * Provides test data for testDataDrivenValidLogin.
     * Reads login_data.json and returns a Stream — one element per parametrized run.
     *
     * Python equivalent: @pytest.fixture reading login_data.json and
     *   @pytest.mark.parametrize("data", test_data)
     */
    static Stream<TestDataReader.LoginData> loginDataProvider() {
        List<TestDataReader.LoginData> data =
            TestDataReader.read("login_data.json", TestDataReader.LoginData.class);
        return data.stream();
    }
}
