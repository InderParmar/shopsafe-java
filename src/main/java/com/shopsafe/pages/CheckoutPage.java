package com.shopsafe.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * CheckoutPage — covers both checkout steps and the confirmation screen.
 * Python equivalent: pages/checkout_page.py
 *
 * Sauce Demo's checkout fields load with class="input_error form_input error".
 * React locks its internal fiber state in error mode — sendKeys and standard
 * JS value-setting are both silently discarded. The only reliable fix is the
 * native HTMLInputElement prototype setter, which bypasses React's proxy.
 * Confirmed working via browser console before implementing here.
 */
public class CheckoutPage extends BasePage {

    private static final By FIRST_NAME_INPUT  = By.id("first-name");
    private static final By LAST_NAME_INPUT   = By.id("last-name");
    private static final By POSTAL_CODE_INPUT = By.id("postal-code");
    private static final By CONTINUE_BUTTON   = By.id("continue");
    private static final By ERROR_MESSAGE     = By.cssSelector("[data-test='error']");
    private static final By ITEM_TOTAL_LABEL  = By.cssSelector(".summary_subtotal_label");
    private static final By TAX_LABEL         = By.cssSelector(".summary_tax_label");
    private static final By TOTAL_LABEL       = By.cssSelector(".summary_total_label");
    private static final By FINISH_BUTTON     = By.id("finish");
    private static final By CONFIRM_HEADER    = By.cssSelector(".complete-header");

    /**
     * Native React setter — bypasses React's value proxy.
     * arguments[0] = the WebElement, arguments[1] = the string value.
     */
    private static final String REACT_SET_VALUE_JS =
        "var nativeSetter = Object.getOwnPropertyDescriptor(" +
        "    window.HTMLInputElement.prototype, 'value').set;" +
        "nativeSetter.call(arguments[0], arguments[1]);" +
        "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));";

    /** JS click — bypasses viewport interactability checks for buttons. */
    private static final String JS_CLICK =
        "arguments[0].click();";

    public CheckoutPage(WebDriver driver) {
        super(driver);
    }

    // ── Step 1 actions ────────────────────────────────────────────────────────

    public CheckoutPage fillInfo(String firstName, String lastName, String postalCode) {
        // Wait for the form to be fully hydrated by React before filling.
        // The page transitions from a loading state to an interactive state —
        // firing the native setter before hydration completes causes the value
        // to be discarded when React reconciles. Waiting for the first field
        // to be both visible AND present in the DOM ensures hydration is done.
        wait.until(ExpectedConditions.visibilityOfElementLocated(FIRST_NAME_INPUT));
        wait.until(ExpectedConditions.presenceOfElementLocated(LAST_NAME_INPUT));
        wait.until(ExpectedConditions.presenceOfElementLocated(POSTAL_CODE_INPUT));

        reactFill(FIRST_NAME_INPUT, firstName);
        reactFill(LAST_NAME_INPUT, lastName);
        reactFill(POSTAL_CODE_INPUT, postalCode);
        return this;
    }

    /**
     * Set a React-controlled input using the native prototype setter.
     * Re-fetches the element after the wait to avoid stale element references.
     */
    private void reactFill(By locator, String value) {
        WebElement field = wait.until(
            ExpectedConditions.visibilityOfElementLocated(locator));
        ((JavascriptExecutor) driver).executeScript(REACT_SET_VALUE_JS, field, value);
    }
    
    public CheckoutPage clickContinue() {
        WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(CONTINUE_BUTTON));
        ((JavascriptExecutor) driver).executeScript(JS_CLICK, btn);
        return this;
    }

    public CheckoutPage fillInfoAndContinue(String firstName, String lastName, String postalCode) {
        fillInfo(firstName, lastName, postalCode);
        clickContinue();
        waitForUrlContaining("checkout-step-two");
        return this;
    }

    public String getErrorMessage() {
        return getText(ERROR_MESSAGE);
    }

    public boolean isErrorDisplayed() {
        return isDisplayed(ERROR_MESSAGE);
    }

    // ── Step 2 ────────────────────────────────────────────────────────────────

    public double getItemTotal() {
        return extractPrice(getText(ITEM_TOTAL_LABEL));
    }

    public double getTax() {
        return extractPrice(getText(TAX_LABEL));
    }

    public double getTotal() {
        return extractPrice(getText(TOTAL_LABEL));
    }

    private double extractPrice(String labelText) {
        return Double.parseDouble(labelText.split("\\$")[1].trim());
    }

    /**
     * Click Finish using JS click — same viewport issue as Continue.
     * The Finish button on step 2 also needs a JS click on Chrome 148.
     */
    public CheckoutPage clickFinish() {
        WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(FINISH_BUTTON));
        ((JavascriptExecutor) driver).executeScript(JS_CLICK, btn);
        waitForUrlContaining("checkout-complete");
        return this;
    }

    // ── Confirmation ──────────────────────────────────────────────────────────

    public String getConfirmationHeader() {
        return getText(CONFIRM_HEADER);
    }
}