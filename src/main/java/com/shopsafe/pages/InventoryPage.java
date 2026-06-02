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
 * Python equivalent: pages/inventory_page.py
 *
 * All button clicks use JavascriptExecutor.click() for Chrome 148 Linux CI
 * compatibility. Chrome 148 headless on Linux rejects standard Selenium clicks
 * on elements it considers outside the interactable viewport area.
 */
public class InventoryPage extends BasePage {

    private static final By SORT_DROPDOWN     = By.cssSelector("[data-test='product-sort-container']");
    private static final By PRODUCT_NAMES     = By.cssSelector(".inventory_item_name");
    private static final By PRODUCT_PRICES    = By.cssSelector(".inventory_item_price");
    private static final By CART_ICON         = By.cssSelector(".shopping_cart_link");
    private static final By CART_BADGE        = By.cssSelector(".shopping_cart_badge");
    private static final By PAGE_TITLE        = By.cssSelector(".title");

    public static final String SORT_AZ         = "az";
    public static final String SORT_ZA         = "za";
    public static final String SORT_PRICE_LOW  = "lohi";
    public static final String SORT_PRICE_HIGH = "hilo";

    private static final String JS_CLICK = "arguments[0].click();";

    public InventoryPage(WebDriver driver) {
        super(driver);
    }

    // ── Sort ─────────────────────────────────────────────────────────────────

    public InventoryPage sortBy(String sortValue) {
        WebElement dropdown = wait.until(
            ExpectedConditions.elementToBeClickable(SORT_DROPDOWN));
        new Select(dropdown).selectByValue(sortValue);
        return this;
    }

    // ── Product data extractors ───────────────────────────────────────────────

    public List<String> getAllProductNames() {
        return driver.findElements(PRODUCT_NAMES)
                     .stream()
                     .map(WebElement::getText)
                     .collect(Collectors.toList());
    }

    public List<Double> getAllProductPrices() {
        return driver.findElements(PRODUCT_PRICES)
                     .stream()
                     .map(e -> Double.parseDouble(e.getText().replace("$", "")))
                     .collect(Collectors.toList());
    }

    // ── Sort validation helpers ───────────────────────────────────────────────

    public List<String> getSortedNamesAZ() {
        return getAllProductNames().stream().sorted().collect(Collectors.toList());
    }

    public List<String> getSortedNamesZA() {
        return getAllProductNames().stream()
                                  .sorted(Comparator.reverseOrder())
                                  .collect(Collectors.toList());
    }

    public List<Double> getSortedPricesLowHigh() {
        return getAllProductPrices().stream().sorted().collect(Collectors.toList());
    }

    public List<Double> getSortedPricesHighLow() {
        return getAllProductPrices().stream()
                                   .sorted(Comparator.reverseOrder())
                                   .collect(Collectors.toList());
    }

    // ── Cart management ───────────────────────────────────────────────────────

    /**
     * Add a product to the cart by its display name using JS click.
     * Standard element.click() fails on Chrome 148 Linux CI for scoped
     * child elements — JS click bypasses the viewport check.
     */
    public InventoryPage addToCartByName(String productName) {
        WebElement itemContainer = getItemContainerByName(productName);
        WebElement btn = itemContainer.findElement(By.cssSelector(".btn_primary"));
        ((JavascriptExecutor) driver).executeScript(JS_CLICK, btn);
        return this;
    }

    /**
     * Remove a product from the cart by its display name using JS click.
     */
    public InventoryPage removeFromCartByName(String productName) {
        WebElement itemContainer = getItemContainerByName(productName);
        WebElement btn = itemContainer.findElement(By.cssSelector(".btn_secondary"));
        ((JavascriptExecutor) driver).executeScript(JS_CLICK, btn);
        return this;
    }

    public int getCartBadgeCount() {
        return Integer.parseInt(getText(CART_BADGE));
    }

    public boolean isCartBadgeDisplayed() {
        return isDisplayed(CART_BADGE);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    public CartPage goToCart() {
        WebElement cartIcon = wait.until(
            ExpectedConditions.presenceOfElementLocated(CART_ICON));
        ((JavascriptExecutor) driver).executeScript(JS_CLICK, cartIcon);
        waitForUrlContaining("cart");
        return new CartPage(driver);
    }

    // ── Page state ────────────────────────────────────────────────────────────

    public String getPageTitle() {
        return getText(PAGE_TITLE);
    }

    public int getProductCount() {
        return driver.findElements(PRODUCT_NAMES).size();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private WebElement getItemContainerByName(String productName) {
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