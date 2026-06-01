package com.shopsafe.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * LoginPage — encapsulates all Selenium interactions on the Sauce Demo login screen.
 *
 * Python equivalent: pages/login_page.py
 *
 * Covers every action needed by the 9 login test cases:
 *  - Valid login → redirects to /inventory.html
 *  - Locked-out user → error banner
 *  - Wrong password → error banner
 *  - Empty username / empty password → field-level validation errors
 *  - Error dismissal via the × button
 *  - Data-driven login (parametrized from JSON)
 *
 * Locator strategy: By.id is preferred whenever an id attribute is present
 * because ids are the most stable selector — they don't break when CSS classes
 * or DOM structure changes. CSS selectors are used only where no id exists.
 */
public class LoginPage extends BasePage {

    // ── Locators ─────────────────────────────────────────────────────────────
    // Declared as private constants so they never leak into the test layer.
    // If Sauce Demo changes a selector, the fix is in one place.

    private static final By USERNAME_INPUT   = By.id("user-name");
    private static final By PASSWORD_INPUT   = By.id("password");
    private static final By LOGIN_BUTTON     = By.id("login-button");

    // The error container wraps both the message text and the dismiss (×) button.
    private static final By ERROR_CONTAINER  = By.cssSelector("[data-test='error']");
    // The × button inside the error banner — clicking it clears the error.
    private static final By ERROR_DISMISS    = By.cssSelector(".error-button");

    // URL fragment used by waitForUrlContaining() after a successful login.
    private static final String INVENTORY_URL_FRAGMENT = "inventory";

    /**
     * Constructor — just passes the driver up to BasePage.
     * No PageFactory here: we use explicit By locators to keep WebDriverWait integrated
     * (PageFactory's @FindBy does *not* wait; it returns a proxy that throws immediately
     * if the element is gone). Explicit By + WebDriverWait is more reliable for dynamic pages.
     */
    public LoginPage(WebDriver driver) {
        super(driver);
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    /**
     * Open the Sauce Demo home page.
     * Called by the test fixture before each login test.
     */
    public LoginPage open(String baseUrl) {
        navigateTo(baseUrl);
        return this; // fluent — lets tests chain: loginPage.open(url).login(user, pass)
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    /**
     * Fill in credentials and click Login.
     * Returns an InventoryPage because a successful login lands on /inventory.html —
     * this is the page-chaining pattern: the return type communicates where you end up.
     *
     * Python equivalent: login_page.login(username, password) → InventoryPage
     */
    public InventoryPage login(String username, String password) {
        type(USERNAME_INPUT, username);
        type(PASSWORD_INPUT, password);
        click(LOGIN_BUTTON);
        // Wait for the redirect before handing back the InventoryPage reference.
        // Without this wait, the caller would get an InventoryPage whose driver
        // is still on the login URL and every subsequent call would fail.
        waitForUrlContaining(INVENTORY_URL_FRAGMENT);
        return new InventoryPage(driver);
    }

    /**
     * Attempt to log in but do NOT wait for a redirect — used for negative tests
     * where we expect the page to stay on login and show an error.
     */
    public LoginPage attemptLogin(String username, String password) {
        type(USERNAME_INPUT, username);
        type(PASSWORD_INPUT, password);
        click(LOGIN_BUTTON);
        return this; // stays on LoginPage — caller will assert the error next
    }

    /**
     * Click the × button to dismiss the error banner.
     * Covered by test_error_message_dismissed_on_click.
     */
    public LoginPage dismissError() {
        click(ERROR_DISMISS);
        return this;
    }

    // ── Assertions helpers ───────────────────────────────────────────────────

    /**
     * Return the text content of the error banner.
     * Tests call this and assert the exact string — e.g.
     *   assertEquals("Epic sadface: Username is required", loginPage.getErrorMessage());
     */
    public String getErrorMessage() {
        return getText(ERROR_CONTAINER);
    }

    /**
     * Return true if the error banner is currently visible.
     * Used by test_error_message_dismissed_on_click to confirm the banner disappears.
     */
    public boolean isErrorDisplayed() {
        return isDisplayed(ERROR_CONTAINER);
    }
}
