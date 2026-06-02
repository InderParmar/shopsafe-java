package com.shopsafe.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * InventoryPage — all Selenium interactions on /inventory.html.
 *
 * Python equivalent: pages/inventory_page.py
 *
 * Covers every action needed by the 9 inventory test cases:
 *  - Page state assertions (title, item count)
 *  - All 4 sort orders (A→Z, Z→A, price low→high, price high→low)
 *    with full-list comparison assertions (not hardcoded item checks)
 *  - Add / remove items by product name
 *  - Cart badge count management
 *  - Navigate to cart → returns CartPage (page-chaining pattern)
 */
public class InventoryPage extends BasePage {

    // ── Locators ─────────────────────────────────────────────────────────────

    // The sort dropdown — a native <select> element, handled with Selenium's Select class.
    private static final By SORT_DROPDOWN         = By.cssSelector("[data-test='product-sort-container']");

    // All product name labels on the inventory grid.
    private static final By PRODUCT_NAMES         = By.cssSelector(".inventory_item_name");

    // All product price labels — text like "$29.99".
    private static final By PRODUCT_PRICES        = By.cssSelector(".inventory_item_price");

    // The shopping cart icon/link in the top-right header.
    private static final By CART_ICON             = By.cssSelector(".shopping_cart_link");

    // The red badge on the cart icon showing item count ("1", "2", etc.).
    private static final By CART_BADGE            = By.cssSelector(".shopping_cart_badge");

    // The page title element — expected to read "Products".
    private static final By PAGE_TITLE            = By.cssSelector(".title");

    // Sort option values that map to the <option value="..."> attributes in the DOM.
    public static final String SORT_AZ            = "az";
    public static final String SORT_ZA            = "za";
    public static final String SORT_PRICE_LOW     = "lohi";
    public static final String SORT_PRICE_HIGH    = "hilo";

    public InventoryPage(WebDriver driver) {
        super(driver);
    }

    // ── Sort ─────────────────────────────────────────────────────────────────

    /**
     * Select a sort option from the dropdown by its value attribute.
     *
     * Why Select class? The sort control is a native HTML <select> element.
     * Selenium's Select class handles it cleanly: it finds the <option> that matches
     * and clicks it — no need for findElement + click loops.
     *
     * @param sortValue one of the SORT_* constants defined above, e.g. SORT_AZ
     */
    public InventoryPage sortBy(String sortValue) {
        WebElement dropdown = wait.until(
            org.openqa.selenium.support.ui.ExpectedConditions
                .elementToBeClickable(SORT_DROPDOWN)
        );
        new Select(dropdown).selectByValue(sortValue);
        return this;
    }

    // ── Product data extractors ───────────────────────────────────────────────

    /**
     * Return all product names currently visible on the page as a List<String>.
     *
     * Python equivalent: inventory_page.get_all_product_names()
     *
     * Java translation:
     *  driver.findElements(PRODUCT_NAMES)  → List<WebElement>
     *  .stream()                            → treat it as a stream for functional ops
     *  .map(WebElement::getText)            → extract the text from each element
     *  .collect(Collectors.toList())        → gather results back into a List<String>
     *
     * The sort assertion in the tests is then: assertEquals(names, sorted(names))
     * which works for any number of products — no hardcoded names needed.
     */
    public List<String> getAllProductNames() {
        return driver.findElements(PRODUCT_NAMES)
                     .stream()
                     .map(WebElement::getText)
                     .collect(Collectors.toList());
    }

    /**
     * Return all product prices as doubles (e.g. 29.99), stripping the "$" prefix.
     *
     * Python equivalent: [float(p.strip("$")) for p in get_all_product_prices()]
     *
     * Java:
     *  .replace("$", "")  → removes the dollar sign from "$29.99" → "29.99"
     *  Double.parseDouble  → converts "29.99" to the primitive double 29.99
     */
    public List<Double> getAllProductPrices() {
        return driver.findElements(PRODUCT_PRICES)
                     .stream()
                     .map(e -> Double.parseDouble(e.getText().replace("$", "")))
                     .collect(Collectors.toList());
    }

    // ── Sort validation helpers ───────────────────────────────────────────────

    /**
     * Return a copy of the names list sorted A→Z.
     * Tests call: assertEquals(page.getSortedNamesAZ(), page.getAllProductNames())
     * after selecting the A→Z sort — this asserts the live DOM matches the expected order
     * without hardcoding any product name.
     */
    public List<String> getSortedNamesAZ() {
        return getAllProductNames().stream()
                                  .sorted()
                                  .collect(Collectors.toList());
    }

    /**
     * Return a copy of the names list sorted Z→A.
     */
    public List<String> getSortedNamesZA() {
        return getAllProductNames().stream()
                                  .sorted(Comparator.reverseOrder())
                                  .collect(Collectors.toList());
    }

    /**
     * Return a copy of the prices list sorted low→high.
     */
    public List<Double> getSortedPricesLowHigh() {
        return getAllProductPrices().stream()
                                   .sorted()
                                   .collect(Collectors.toList());
    }

    /**
     * Return a copy of the prices list sorted high→low.
     */
    public List<Double> getSortedPricesHighLow() {
        return getAllProductPrices().stream()
                                   .sorted(Comparator.reverseOrder())
                                   .collect(Collectors.toList());
    }

    // ── Cart management ───────────────────────────────────────────────────────

    /**
     * Add a product to the cart by its display name.
     *
     * Strategy: find the product container that contains the target name,
     * then click the Add-to-cart button scoped to that container.
     * This is safer than using a global By.cssSelector(".btn_primary") because
     * that would always click the first product's button, not the named one.
     *
     * Python equivalent: inventory_page.add_item_to_cart(name)
     */
    public InventoryPage addToCartByName(String productName) {
        // Find the inventory item div that contains this product's name label.
        WebElement itemContainer = getItemContainerByName(productName);
        // The Add to cart button inside that container.
        itemContainer.findElement(By.cssSelector(".btn_primary")).click();
        return this;
    }

    /**
     * Remove a product from the cart (when it's already added and shows "Remove").
     */
    public InventoryPage removeFromCartByName(String productName) {
        WebElement itemContainer = getItemContainerByName(productName);
        itemContainer.findElement(By.cssSelector(".btn_secondary")).click();
        return this;
    }

    /**
     * Return the numeric value shown on the cart badge (e.g. "2" → 2).
     * Used to assert item count after add/remove operations.
     */
    public int getCartBadgeCount() {
        return Integer.parseInt(getText(CART_BADGE));
    }

    /**
     * Return true if the cart badge is visible (i.e. at least one item is in the cart).
     */
    public boolean isCartBadgeDisplayed() {
        return isDisplayed(CART_BADGE);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    /**
     * Click the cart icon and return a CartPage instance.
     *
     * Page-chaining pattern: the return type tells the caller exactly where they'll land.
     * Python equivalent: inventory_page.go_to_cart() → CartPage
     */
    public CartPage goToCart() {
        WebElement cartIcon = wait.until(
            ExpectedConditions.presenceOfElementLocated(CART_ICON));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cartIcon);
        waitForUrlContaining("cart");
        return new CartPage(driver);
    }
    // ── Page state ────────────────────────────────────────────────────────────

    /**
     * Return the page title text. Expected: "Products".
     */
    public String getPageTitle() {
        return getText(PAGE_TITLE);
    }

    /**
     * Return the number of products currently visible on the page.
     */
    public int getProductCount() {
        return driver.findElements(PRODUCT_NAMES).size();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Find and return the inventory item container element that contains the given product name.
     *
     * This is used by addToCartByName() and removeFromCartByName() to scope button clicks
     * to a specific product — avoiding accidental clicks on the wrong item.
     *
     * Approach: iterate all inventory items and find the one whose name label matches.
     */
    private WebElement getItemContainerByName(String productName) {
        // .inventory_item wraps the entire product tile (name, description, price, button).
        List<WebElement> items = driver.findElements(By.cssSelector(".inventory_item"));
        for (WebElement item : items) {
            String name = item.findElement(By.cssSelector(".inventory_item_name")).getText();
            if (name.equals(productName)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Product not found on page: " + productName);
    }
}
