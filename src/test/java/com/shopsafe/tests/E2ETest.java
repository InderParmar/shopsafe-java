package com.shopsafe.tests;

import com.shopsafe.pages.CartPage;
import com.shopsafe.pages.CheckoutPage;
import com.shopsafe.pages.InventoryPage;
import com.shopsafe.pages.LoginPage;
import com.shopsafe.utils.ConfigReader;
import com.shopsafe.utils.TestDataReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * E2ETest — 2 end-to-end test cases covering the full purchase flow.
 *
 * Python equivalent: tests/test_e2e.py
 *
 * These tests exercise the entire user journey:
 *  Login → Inventory → Cart → Checkout Step 1 → Step 2 → Confirmation
 *
 * No @BeforeEach here — each test starts from scratch (fresh login)
 * to ensure complete independence. E2E tests should never share state.
 */
@DisplayName("End-to-End Module")
class E2ETest extends BaseTest {

    // ── Full purchase flow ────────────────────────────────────────────────────

    /**
     * Happy path: complete the entire purchase flow for one product.
     *
     * Python equivalent: test_full_purchase_flow in test_e2e.py
     *
     * This test reads like a user story because of page chaining:
     *  1. Open login page
     *  2. Login → get InventoryPage
     *  3. Add item → navigate to cart → get CartPage
     *  4. Proceed to checkout → get CheckoutPage
     *  5. Fill info → continue → finish
     *  6. Assert confirmation
     */
    @Test
    @DisplayName("Full purchase flow: login → add item → checkout → confirm")
    void testFullPurchaseFlow() {
        // Step 1: Login
        InventoryPage inventory = new LoginPage(driver)
            .open(baseUrl)
            .login(ConfigReader.getStandardUsername(), ConfigReader.getPassword());

        // Step 2: Verify we're on the inventory page
        assertEquals("Products", inventory.getPageTitle());

        // Step 3: Add a product and navigate to cart
        CartPage cart = inventory
            .addToCartByName("Sauce Labs Backpack")
            .goToCart();

        // Step 4: Verify item is in cart
        assertTrue(cart.isItemInCart("Sauce Labs Backpack"),
                   "Item should be present in cart before checkout");
        assertEquals(1, cart.getCartItemCount());

        // Step 5: Proceed through checkout
        CheckoutPage checkout = cart.proceedToCheckout();
        checkout.fillInfoAndContinue("John", "Doe", "M5V2T6");

        // Step 6: Verify math on step 2 before placing order
        double itemTotal = checkout.getItemTotal();
        double tax       = checkout.getTax();
        double total     = checkout.getTotal();
        assertEquals(itemTotal + tax, total, 0.01, "Total should equal subtotal + tax");

        // Step 7: Finish and verify confirmation
        checkout.clickFinish();
        assertEquals("Thank you for your order!", checkout.getConfirmationHeader(),
                     "Confirmation header should appear after completing purchase");
        assertTrue(driver.getCurrentUrl().contains("checkout-complete"));
    }

    // ── Data-driven checkout ──────────────────────────────────────────────────

    /**
     * Data-driven checkout test — reads from checkout_data.json.
     *
     * Python equivalent: test_full_purchase_flow_data_driven in test_e2e.py,
     *   parametrized via @pytest.mark.parametrize reading e2e_data.json
     *
     * The "expected" field in CheckoutData drives two code paths:
     *  - "success"   → confirm order lands on checkout-complete
     *  - "zip_error" → confirm error message for missing postal code
     *
     * This is the same branching logic as the Python version's
     *   if data["expected"] == "success": ... else: ...
     */
    @ParameterizedTest(name = "Data-driven checkout: {0}")
    @MethodSource("checkoutDataProvider")
    @DisplayName("Data-driven: checkout with success and error paths")
    void testDataDrivenCheckout(TestDataReader.CheckoutData data) {
        // All data-driven runs start with a fresh login + item in cart.
        CheckoutPage checkout = new LoginPage(driver)
            .open(baseUrl)
            .login(ConfigReader.getStandardUsername(), ConfigReader.getPassword())
            .addToCartByName("Sauce Labs Backpack")
            .goToCart()
            .proceedToCheckout();

        checkout.fillInfo(data.firstName, data.lastName, data.postalCode);
        checkout.clickContinue();

        if ("success".equals(data.expected)) {
            // Happy path: proceed through step 2 and finish.
            // waitForUrlContaining was already called inside fillInfoAndContinue,
            // but here we called fillInfo + clickContinue separately, so assert the URL.
            assertTrue(driver.getCurrentUrl().contains("checkout-step-two"),
                       "Valid data should advance to step 2");

            checkout.clickFinish();
            assertEquals("Thank you for your order!", checkout.getConfirmationHeader());

        } else if ("zip_error".equals(data.expected)) {
            // Error path: missing postal code should show a validation error.
            assertTrue(checkout.isErrorDisplayed(),
                       "Empty postal code should show an error banner");
            assertTrue(checkout.getErrorMessage().contains("Postal Code is required"),
                       "Error message should mention 'Postal Code is required'");
        }
    }

    /**
     * Reads checkout_data.json and returns a Stream for @ParameterizedTest.
     *
     * checkout_data.json format:
     * [
     *   { "firstName": "John",  "lastName": "Doe",   "postalCode": "M5V2T6", "expected": "success"   },
     *   { "firstName": "Jane",  "lastName": "Smith",  "postalCode": "",       "expected": "zip_error" }
     * ]
     */
    static Stream<TestDataReader.CheckoutData> checkoutDataProvider() {
        List<TestDataReader.CheckoutData> data =
            TestDataReader.read("checkout_data.json", TestDataReader.CheckoutData.class);
        return data.stream();
    }
}
