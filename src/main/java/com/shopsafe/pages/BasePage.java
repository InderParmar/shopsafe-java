package com.shopsafe.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * BasePage — shared Selenium helpers inherited by every page class.
 * Python equivalent: pages/base_page.py
 */
public class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;

    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
    }

    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    /**
     * Type text into a field.
     *
     * Chrome 148 introduced stricter interactability checks — elements outside
     * the visible viewport throw ElementNotInteractableException even when
     * visibilityOfElementLocated() passes. scrollIntoView({block:'center'})
     * moves the element to the middle of the screen before sendKeys fires.
     * This affects both Python and Java on Chrome 148 equally.
     */
    protected void type(By locator, String text) {
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block: 'center'});", element);
        // Brief pause after scroll so Chrome finishes repainting before input.
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        element.click();
        element.clear();
        element.sendKeys(text);
    }

    protected String getText(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).getText();
    }

    protected String getTextFromElement(By locator) {
        return getText(locator);
    }

    protected boolean isDisplayed(By locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    protected WebElement findElement(SearchContext parent, By locator) {
        return parent.findElement(locator);
    }

    protected void navigateTo(String url) {
        driver.get(url);
    }

    protected String getTitle() {
        return driver.getTitle();
    }

    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    protected void waitForUrlContaining(String urlFragment) {
        wait.until(ExpectedConditions.urlContains(urlFragment));
    }
}