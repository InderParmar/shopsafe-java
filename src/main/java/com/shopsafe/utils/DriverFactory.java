package com.shopsafe.utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * DriverFactory — creates and configures WebDriver instances.
 * Python equivalent: conftest.py driver fixture.
 */
public class DriverFactory {

    private DriverFactory() {}

    public static WebDriver createDriver() {
        String browser   = System.getProperty("browser", "chrome").toLowerCase();
        boolean headless = isHeadless();
        return switch (browser) {
            case "firefox" -> createFirefox(headless);
            default        -> createChrome(headless);
        };
    }

    private static WebDriver createChrome(boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        if (headless) {
            options.addArguments("--headless=new");
        }

        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--password-store=basic");
        options.addArguments("--disable-save-password-bubble");

        // Chrome 148 headless uses a very small default window (800x600).
        // The Sauce Demo checkout form fields fall outside the interactable
        // viewport at that size. Setting a full HD window size ensures elements
        // are in the visible viewport without needing scrollIntoView as a crutch.
        // scrollIntoView is kept as a safety net but this prevents most failures.
        options.addArguments("--window-size=1920,1080");

        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        return new ChromeDriver(options);
    }

    private static WebDriver createFirefox(boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("--headless");
            options.addArguments("--width=1920");
            options.addArguments("--height=1080");
        }
        return new FirefoxDriver(options);
    }

    private static boolean isHeadless() {
        boolean inCI        = System.getenv("CI") != null;
        boolean headlessFlag = Boolean.parseBoolean(System.getProperty("headless", "false"));
        return inCI || headlessFlag;
    }
}