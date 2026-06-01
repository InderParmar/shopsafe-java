package com.shopsafe.tests;

import com.shopsafe.pages.CheckoutPage;
import com.shopsafe.pages.LoginPage;
import com.shopsafe.utils.ConfigReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CheckoutTest — 9 test cases covering the Sauce Demo checkout module.
 *
 * Python equivalent: tests/test_checkout.py
 *
 * Key assertions carried from Python:
 *
 * 1. Step 1 form validation — empty fields trigger specific error messages.
 *
 * 2. Float math assertion with tolerance:
 *    abs(total - (itemTotal + tax)) < 0.01
 *    This is identical to the Python assertion. Using == on doubles is unreliable
 *    because IEEE 754 floating-point arithmetic can produce 43.179999... instead
 *    of 43.18. The 0.01 tolerance catches real calculation bugs while ignoring
 *    floating-point noise.
 *
 * 3. Order confirmation — page shows "Thank you for your order!"
 */
@DisplayName("Checkout Module")
class CheckoutTest extends BaseTest {

    private CheckoutPage checkoutPage;

    /**
     * Log in, add an item, navigate through cart to arrive at checkout step 1.
     * All 9 checkout tests start from this state.
     */
    @BeforeEach
    void setUp() {
        checkoutPage = new LoginPage(driver)
            .open(baseUrl)
            .login(ConfigReader.getStandardUsername(), ConfigReader.getPassword())
            .addToCartByName("Sauce Labs Backpack")
            .goToCart()
            .proceedToCheckout();
    }

    // ── Step 1: form validation ───────────────────────────────────────────────

    @Test
    @DisplayName("Empty first name → error: 'First Name is required'")
    void testEmptyFirstName() {
        checkoutPage.fillInfo("", "Doe", "M5V2T6").clickContinue();
        assertTrue(checkoutPage.isErrorDisplayed(), "Error banner should be visible");
        assertTrue(checkoutPage.getErrorMessage().contains("First Name is required"));
    }

    @Test
    @DisplayName("Empty last name → error: 'Last Name is required'")
    void testEmptyLastName() {
        checkoutPage.fillInfo("John", "", "M5V2T6").clickContinue();
        assertTrue(checkoutPage.isErrorDisplayed());
        assertTrue(checkoutPage.getErrorMessage().contains("Last Name is required"));
    }

    @Test
    @DisplayName("Empty postal code → error: 'Postal Code is required'")
    void testEmptyPostalCode() {
        checkoutPage.fillInfo("John", "Doe", "").clickContinue();
        assertTrue(checkoutPage.isErrorDisplayed());
        assertTrue(checkoutPage.getErrorMessage().contains("Postal Code is required"));
    }

    @Test
    @DisplayName("All fields empty → error: 'First Name is required'")
    void testAllFieldsEmpty() {
        checkoutPage.fillInfo("", "", "").clickContinue();
        assertTrue(checkoutPage.getErrorMessage().contains("First Name is required"));
    }

    // ── Step 2: price math assertions ─────────────────────────────────────────

    /**
     * Advance to step 2 and assert subtotal + tax = total (within floating-point tolerance).
     *
     * Python equivalent:
     *   item_total = checkout_page.get_item_total()
     *   tax        = checkout_page.get_tax()
     *   total      = checkout_page.get_total()
     *   assert abs(total - (item_total + tax)) < 0.01
     *
     * Java translation:
     *   assertEquals(expected, actual, delta)
     *   where delta = 0.01 — JUnit 5's built-in double comparison with tolerance.
     *   This is more idiomatic than asserting abs(total - sum) < 0.01 manually.
     */
    @Test
    @DisplayName("Step 2: total = item_total + tax (within 0.01 tolerance)")
    void testTotalEqualsSubtotalPlusTax() {
        checkoutPage.fillInfoAndContinue("John", "Doe", "M5V2T6");

        double itemTotal = checkoutPage.getItemTotal();
        double tax       = checkoutPage.getTax();
        double total     = checkoutPage.getTotal();

        // Assert the math holds within floating-point tolerance.
        // 0.01 tolerance: catches real errors (off by $1) but ignores IEEE 754 noise.
        assertEquals(itemTotal + tax, total, 0.01,
                     String.format("total (%.2f) should equal itemTotal (%.2f) + tax (%.2f)",
                                   total, itemTotal, tax));
    }

    @Test
    @DisplayName("Step 2: item total is a positive number")
    void testItemTotalIsPositive() {
        checkoutPage.fillInfoAndContinue("John", "Doe", "M5V2T6");
        assertTrue(checkoutPage.getItemTotal() > 0,
                   "Item total should be greater than zero");
    }

    @Test
    @DisplayName("Step 2: tax is a positive number")
    void testTaxIsPositive() {
        checkoutPage.fillInfoAndContinue("John", "Doe", "M5V2T6");
        assertTrue(checkoutPage.getTax() > 0, "Tax should be greater than zero");
    }

    @Test
    @DisplayName("Step 2: grand total is greater than item total (tax is added)")
    void testTotalGreaterThanItemTotal() {
        checkoutPage.fillInfoAndContinue("John", "Doe", "M5V2T6");
        assertTrue(checkoutPage.getTotal() > checkoutPage.getItemTotal(),
                   "Grand total must exceed item subtotal because tax is added");
    }

    // ── Order confirmation ────────────────────────────────────────────────────

    @Test
    @DisplayName("Completing checkout shows 'Thank you for your order!'")
    void testOrderConfirmation() {
        checkoutPage
            .fillInfoAndContinue("John", "Doe", "M5V2T6")
            .clickFinish();

        assertEquals("Thank you for your order!", checkoutPage.getConfirmationHeader(),
                     "Confirmation header should show the success message");
    }

    @Test
    @DisplayName("Completing checkout lands on checkout-complete URL")
    void testConfirmationUrl() {
        checkoutPage
            .fillInfoAndContinue("John", "Doe", "M5V2T6")
            .clickFinish();

        assertTrue(driver.getCurrentUrl().contains("checkout-complete"),
                   "URL should contain 'checkout-complete' after finishing checkout");
    }
}
