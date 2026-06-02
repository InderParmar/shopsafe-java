package com.shopsafe.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.stream.Collectors;

/**
 * CartPage — all Selenium interactions on /cart.html.
 * Python equivalent: pages/cart_page.py
 *
 * All button clicks use JavascriptExecutor.click() for Chrome 148 Linux CI
 * compatibility.
 */
public class CartPage extends BasePage {

    private static final By CART_ITEMS        = By.cssSelector(".cart_item");
    private static final By ITEM_NAME         = By.cssSelector(".inventory_item_name");
    private static final By ITEM_PRICE        = By.cssSelector(".inventory_item_price");
    private static final By REMOVE_BUTTON     = By.cssSelector(".cart_button");
    private static final By CONTINUE_SHOPPING = By.id("continue-shopping");
    private static final By CHECKOUT_BUTTON   = By.id("checkout");

    private static final String JS_CLICK = "arguments[0].click();";

    public CartPage(WebDriver driver) {
        super(driver);
    }

    // ── Item queries ─────────────────────────────────────────────────────────

    public List<String> getCartItemNames() {
        return driver.findElements(CART_ITEMS)
                     .stream()
                     .map(item -> item.findElement(ITEM_NAME).getText())
                     .collect(Collectors.toList());
    }

    public int getCartItemCount() {
        return driver.findElements(CART_ITEMS).size();
    }

    /**
     * Scoped price lookup — reads price from the same row as the matching name.
     * Prevents returning the wrong price when DOM order varies.
     */
    public double getPriceByName(String productName) {
        List<WebElement> items = driver.findElements(CART_ITEMS);
        for (WebElement item : items) {
            String name = item.findElement(ITEM_NAME).getText();
            if (name.equals(productName)) {
                String priceText = item.findElement(ITEM_PRICE).getText();
                return Double.parseDouble(priceText.replace("$", ""));
            }
        }
        throw new IllegalArgumentException("Item not found in cart: " + productName);
    }

    public boolean isItemInCart(String productName) {
        return driver.findElements(CART_ITEMS)
                     .stream()
                     .anyMatch(item -> item.findElement(ITEM_NAME).getText().equals(productName));
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /**
     * Remove a specific item by name using JS click.
     * The Remove button is a scoped child element — standard click fails on
     * Chrome 148 Linux CI for child elements not at the top of the viewport.
     */
    public CartPage removeItemByName(String productName) {
        List<WebElement> items = driver.findElements(CART_ITEMS);
        for (WebElement item : items) {
            String name = item.findElement(ITEM_NAME).getText();
            if (name.equals(productName)) {
                WebElement btn = item.findElement(REMOVE_BUTTON);
                ((JavascriptExecutor) driver).executeScript(JS_CLICK, btn);
                return this;
            }
        }
        throw new IllegalArgumentException("Item not found in cart: " + productName);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    public InventoryPage continueShopping() {
        WebElement btn = wait.until(
            ExpectedConditions.presenceOfElementLocated(CONTINUE_SHOPPING));
        ((JavascriptExecutor) driver).executeScript(JS_CLICK, btn);
        waitForUrlContaining("inventory");
        return new InventoryPage(driver);
    }

    public CheckoutPage proceedToCheckout() {
        WebElement btn = wait.until(
            ExpectedConditions.presenceOfElementLocated(CHECKOUT_BUTTON));
        ((JavascriptExecutor) driver).executeScript(JS_CLICK, btn);
        waitForUrlContaining("checkout-step-one");
        return new CheckoutPage(driver);
    }
}