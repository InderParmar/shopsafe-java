package com.shopsafe.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
/**
 * CartPage — all Selenium interactions on /cart.html.
 *
 * Python equivalent: pages/cart_page.py
 *
 * Covers every action needed by the 6 cart test cases:
 *  - Item display (name and price visible in cart)
 *  - Item count validation (matching badge count from InventoryPage)
 *  - Price consistency between inventory and cart (scoped DOM search)
 *  - Remove item from cart
 *  - Navigate back to inventory
 *  - Proceed to checkout → returns CheckoutPage (page-chaining)
 */
public class CartPage extends BasePage {

    // ── Locators ─────────────────────────────────────────────────────────────

    // Each row in the cart — wraps name, quantity, price, and remove button for one product.
    private static final By CART_ITEMS        = By.cssSelector(".cart_item");

    // Product name label within a cart item row.
    private static final By ITEM_NAME         = By.cssSelector(".inventory_item_name");

    // Price label within a cart item row — text like "$29.99".
    private static final By ITEM_PRICE        = By.cssSelector(".inventory_item_price");

    // The "Remove" button inside a cart item row.
    private static final By REMOVE_BUTTON     = By.cssSelector(".cart_button");

    // "Continue Shopping" — navigates back to /inventory.html.
    private static final By CONTINUE_SHOPPING = By.id("continue-shopping");

    // "Checkout" — advances to the checkout information form.
    private static final By CHECKOUT_BUTTON   = By.id("checkout");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    // ── Item queries ─────────────────────────────────────────────────────────

    /**
     * Return all product names currently in the cart.
     * Used to assert that items added on InventoryPage are present here.
     */
    public List<String> getCartItemNames() {
        return driver.findElements(CART_ITEMS)
                     .stream()
                     .map(item -> item.findElement(ITEM_NAME).getText())
                     .collect(Collectors.toList());
    }

    /**
     * Return the number of line items visible in the cart.
     * Used to assert count matches the inventory badge count.
     */
    public int getCartItemCount() {
        return driver.findElements(CART_ITEMS).size();
    }

    /**
     * Return the price of a specific item by product name, as a double.
     *
     * This is the scoped DOM traversal pattern from Python's get_price_by_name().
     *
     * Problem it solves: driver.findElement(ITEM_PRICE) returns the *first* price
     * in the DOM, which belongs to the first product — not necessarily the one you asked for.
     *
     * Solution: iterate all cart rows, find the one whose name matches, then read
     * the price from *that* row. The name and price are guaranteed to belong to the
     * same product because they're both children of the same cart_item container.
     *
     * Python equivalent:
     *   parent = self.driver.find_element(By.CSS_SELECTOR, f'...')
     *   price  = parent.find_element(By.CSS_SELECTOR, '.inventory_item_price').text
     *
     * @param productName the display name shown in the cart (e.g. "Sauce Labs Backpack")
     * @return price as a double (e.g. 29.99), with the "$" stripped
     */
    public double getPriceByName(String productName) {
        List<WebElement> items = driver.findElements(CART_ITEMS);
        for (WebElement item : items) {
            // Read the name from this specific row.
            String name = item.findElement(ITEM_NAME).getText();
            if (name.equals(productName)) {
                // Read the price from the same row — scoped, not global.
                String priceText = item.findElement(ITEM_PRICE).getText();
                return Double.parseDouble(priceText.replace("$", ""));
            }
        }
        throw new IllegalArgumentException("Item not found in cart: " + productName);
    }

    /**
     * Return true if a specific product name is present in the cart.
     */
    public boolean isItemInCart(String productName) {
        return driver.findElements(CART_ITEMS)
                     .stream()
                     .anyMatch(item -> item.findElement(ITEM_NAME).getText().equals(productName));
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /**
     * Remove a specific item from the cart by product name.
     * Finds the cart row that matches the name, then clicks its Remove button.
     */
    public CartPage removeItemByName(String productName) {
        List<WebElement> items = driver.findElements(CART_ITEMS);
        for (WebElement item : items) {
            String name = item.findElement(ITEM_NAME).getText();
            if (name.equals(productName)) {
                item.findElement(REMOVE_BUTTON).click();
                return this;
            }
        }
        throw new IllegalArgumentException("Item not found in cart: " + productName);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    /**
     * Click "Continue Shopping" and return an InventoryPage.
     * Page-chaining — tells the caller they'll be back on the products list.
     */
    public InventoryPage continueShopping() {
        click(CONTINUE_SHOPPING);
        waitForUrlContaining("inventory");
        return new InventoryPage(driver);
    }

    /**
     * Click "Checkout" and return a CheckoutPage.
     *
     * Python equivalent: cart_page.proceed_to_checkout() → CheckoutPage
     */
    public CheckoutPage proceedToCheckout() {
        WebElement btn = wait.until(
            ExpectedConditions.presenceOfElementLocated(CHECKOUT_BUTTON));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        waitForUrlContaining("checkout-step-one");
        return new CheckoutPage(driver);
    }
}
