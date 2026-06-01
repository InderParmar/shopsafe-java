package com.shopsafe.tests;

import com.shopsafe.pages.CartPage;
import com.shopsafe.pages.InventoryPage;
import com.shopsafe.pages.LoginPage;
import com.shopsafe.utils.ConfigReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CartTest — 6 test cases covering the Sauce Demo cart module.
 *
 * Python equivalent: tests/test_cart.py
 *
 * Key assertion carried from Python:
 *  - Price consistency uses scoped DOM traversal (getPriceByName) so the name
 *    and price are guaranteed to belong to the same product container.
 *    This is the Java equivalent of Python's `parent`-scoped find_element.
 */
@DisplayName("Cart Module")
class CartTest extends BaseTest {

    private InventoryPage inventoryPage;

    private static final String BACKPACK    = "Sauce Labs Backpack";
    private static final String BIKE_LIGHT  = "Sauce Labs Bike Light";
    private static final double BACKPACK_PRICE = 29.99;

    /**
     * Log in, add two items to the cart, then navigate to the cart page.
     * All 6 cart tests start from this state.
     */
    @BeforeEach
    void setUp() {
        inventoryPage = new LoginPage(driver)
            .open(baseUrl)
            .login(ConfigReader.getStandardUsername(), ConfigReader.getPassword());
    }

    // ── Item display ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Added item appears in cart by name")
    void testItemDisplayedInCart() {
        CartPage cartPage = inventoryPage
            .addToCartByName(BACKPACK)
            .goToCart();

        assertTrue(cartPage.isItemInCart(BACKPACK),
                   "Backpack should be present in the cart after adding it");
    }

    @Test
    @DisplayName("Cart item count matches number of items added")
    void testCartItemCountMatchesAdded() {
        CartPage cartPage = inventoryPage
            .addToCartByName(BACKPACK)
            .addToCartByName(BIKE_LIGHT)
            .goToCart();

        assertEquals(2, cartPage.getCartItemCount(),
                     "Cart should contain exactly 2 items after adding 2 products");
    }

    // ── Price consistency ─────────────────────────────────────────────────────

    /**
     * Assert the price shown in the cart matches the price on the inventory page.
     *
     * This uses getPriceByName() which performs a scoped DOM search:
     *  - It finds the cart row that contains "Sauce Labs Backpack"
     *  - Then reads the price from *that row*, not from the first price in the DOM
     *
     * Python equivalent:
     *   cart_price = cart_page.get_price_by_name("Sauce Labs Backpack")
     *   assert inventory_price == cart_price
     *
     * Why 0.001 delta? Double.parseDouble("29.99") may produce 29.989999... due to
     * IEEE 754 floating-point representation. The delta allows for that imprecision
     * while still catching real discrepancies.
     */
    @Test
    @DisplayName("Item price in cart matches price on inventory page")
    void testPriceConsistencyBetweenInventoryAndCart() {
        CartPage cartPage = inventoryPage
            .addToCartByName(BACKPACK)
            .goToCart();

        double cartPrice = cartPage.getPriceByName(BACKPACK);
        assertEquals(BACKPACK_PRICE, cartPrice, 0.001,
                     "Cart price should match the inventory page price for " + BACKPACK);
    }

    // ── Remove item ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Removing an item from cart removes it from the list")
    void testRemoveItemFromCart() {
        CartPage cartPage = inventoryPage
            .addToCartByName(BACKPACK)
            .addToCartByName(BIKE_LIGHT)
            .goToCart();

        // Remove one item.
        cartPage.removeItemByName(BACKPACK);

        // Assert it's gone and only one item remains.
        assertFalse(cartPage.isItemInCart(BACKPACK),
                    "Backpack should not be in cart after removal");
        assertEquals(1, cartPage.getCartItemCount(),
                     "One item should remain in cart after removing one of two");
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("'Continue Shopping' returns to inventory page")
    void testContinueShoppingReturnsToInventory() {
        CartPage cartPage = inventoryPage
            .addToCartByName(BACKPACK)
            .goToCart();

        cartPage.continueShopping();

        assertTrue(driver.getCurrentUrl().contains("inventory"),
                   "URL should contain 'inventory' after clicking 'Continue Shopping'");
    }

    @Test
    @DisplayName("'Checkout' button advances to checkout step 1")
    void testCheckoutButtonAdvancesToCheckout() {
        CartPage cartPage = inventoryPage
            .addToCartByName(BACKPACK)
            .goToCart();

        cartPage.proceedToCheckout();

        assertTrue(driver.getCurrentUrl().contains("checkout-step-one"),
                   "URL should contain 'checkout-step-one' after clicking Checkout");
    }
}
