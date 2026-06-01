package com.shopsafe.tests;

import com.shopsafe.pages.InventoryPage;
import com.shopsafe.pages.LoginPage;
import com.shopsafe.utils.ConfigReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InventoryTest — 9 test cases covering the Sauce Demo inventory/products module.
 *
 * Python equivalent: tests/test_inventory.py
 *
 * Key testing patterns carried over from Python:
 *
 * 1. Sort validation via full-list comparison — not hardcoded product names.
 *    getAllProductNames() → compare against sorted copy.
 *    Works for any number of products and never needs updating.
 *
 * 2. Price sort uses Double comparison, not String comparison.
 *    "$10.00" < "$9.99" alphabetically — sorting as doubles is correct.
 *
 * 3. Cart badge count is asserted as an integer, not a string.
 */
@DisplayName("Inventory Module")
class InventoryTest extends BaseTest {

    private InventoryPage inventoryPage;

    /**
     * Log in and land on the inventory page before each test.
     *
     * Python equivalent: the @pytest.fixture "logged_in_driver" that logs in
     *   and returns an InventoryPage instance.
     */
    @BeforeEach
    void setUp() {
        LoginPage loginPage = new LoginPage(driver);
        inventoryPage = loginPage
            .open(baseUrl)
            .login(ConfigReader.getStandardUsername(), ConfigReader.getPassword());
    }

    // ── Page state ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Inventory page title is 'Products'")
    void testPageTitle() {
        assertEquals("Products", inventoryPage.getPageTitle());
    }

    @Test
    @DisplayName("Inventory page displays exactly 6 products")
    void testProductCount() {
        assertEquals(6, inventoryPage.getProductCount(),
                     "Sauce Demo always has 6 products on the inventory page");
    }

    // ── Sort: A → Z ───────────────────────────────────────────────────────────

    /**
     * After selecting A→Z sort, the displayed order matches the alphabetically sorted list.
     *
     * Python equivalent:
     *   inventory_page.sort_products("az")
     *   names = inventory_page.get_all_product_names()
     *   assert names == sorted(names)
     *
     * Java translation:
     *   sortBy(SORT_AZ)         → triggers the dropdown selection
     *   getAllProductNames()     → reads the live DOM order
     *   getSortedNamesAZ()      → returns a sorted-copy of the same names
     *   assertEquals(sorted, actual) → full-list comparison, not individual elements
     */
    @Test
    @DisplayName("Sort A→Z: products displayed in alphabetical order")
    void testSortAZ() {
        inventoryPage.sortBy(InventoryPage.SORT_AZ);

        List<String> actual = inventoryPage.getAllProductNames();
        List<String> expected = inventoryPage.getSortedNamesAZ();

        assertEquals(expected, actual,
                     "Product names should be in A→Z order after selecting that sort option");
    }

    // ── Sort: Z → A ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Sort Z→A: products displayed in reverse alphabetical order")
    void testSortZA() {
        inventoryPage.sortBy(InventoryPage.SORT_ZA);

        List<String> actual   = inventoryPage.getAllProductNames();
        List<String> expected = inventoryPage.getSortedNamesZA();

        assertEquals(expected, actual,
                     "Product names should be in Z→A order after selecting that sort option");
    }

    // ── Sort: price low → high ────────────────────────────────────────────────

    /**
     * After selecting price low→high, prices match the numerically sorted list.
     *
     * Python equivalent:
     *   prices = [float(p.strip("$")) for p in inventory_page.get_all_product_prices()]
     *   assert prices == sorted(prices)
     *
     * This assertion works because getAllProductPrices() already strips "$" and
     * parses to doubles, so List.equals() compares numbers — not strings.
     */
    @Test
    @DisplayName("Sort price low→high: prices in ascending order")
    void testSortPriceLowToHigh() {
        inventoryPage.sortBy(InventoryPage.SORT_PRICE_LOW);

        List<Double> actual   = inventoryPage.getAllProductPrices();
        List<Double> expected = inventoryPage.getSortedPricesLowHigh();

        assertEquals(expected, actual,
                     "Prices should be in ascending order after low→high sort");
    }

    // ── Sort: price high → low ────────────────────────────────────────────────

    @Test
    @DisplayName("Sort price high→low: prices in descending order")
    void testSortPriceHighToLow() {
        inventoryPage.sortBy(InventoryPage.SORT_PRICE_HIGH);

        List<Double> actual   = inventoryPage.getAllProductPrices();
        List<Double> expected = inventoryPage.getSortedPricesHighLow();

        assertEquals(expected, actual,
                     "Prices should be in descending order after high→low sort");
    }

    // ── Cart badge management ─────────────────────────────────────────────────

    @Test
    @DisplayName("Cart badge not shown when no items added")
    void testCartBadgeNotDisplayedInitially() {
        assertFalse(inventoryPage.isCartBadgeDisplayed(),
                    "Cart badge should not be visible before any item is added");
    }

    @Test
    @DisplayName("Cart badge shows count 1 after adding one item")
    void testCartBadgeShowsOneAfterAddingItem() {
        inventoryPage.addToCartByName("Sauce Labs Backpack");
        assertTrue(inventoryPage.isCartBadgeDisplayed());
        assertEquals(1, inventoryPage.getCartBadgeCount());
    }

    @Test
    @DisplayName("Cart badge shows count 2 after adding two items")
    void testCartBadgeCountIncrements() {
        inventoryPage.addToCartByName("Sauce Labs Backpack");
        inventoryPage.addToCartByName("Sauce Labs Bike Light");
        assertEquals(2, inventoryPage.getCartBadgeCount());
    }

    @Test
    @DisplayName("Cart badge disappears after removing the only item")
    void testCartBadgeHiddenAfterRemoval() {
        inventoryPage.addToCartByName("Sauce Labs Backpack");
        assertTrue(inventoryPage.isCartBadgeDisplayed(), "Badge should appear after adding");

        inventoryPage.removeFromCartByName("Sauce Labs Backpack");
        assertFalse(inventoryPage.isCartBadgeDisplayed(), "Badge should disappear after removing");
    }
}
