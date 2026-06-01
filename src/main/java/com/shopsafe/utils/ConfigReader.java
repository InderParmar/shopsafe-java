package com.shopsafe.utils;

/**
 * ConfigReader — centralises all runtime configuration values.
 *
 * Python equivalent: utils/config_reader.py + config/config.ini
 *
 * In the Python version, config.ini holds browser, base_url, timeouts, and credentials,
 * and config_reader.py reads them with configparser.
 *
 * In Java, the Maven Surefire plugin passes these values as JVM system properties
 * (see pom.xml's <systemPropertyVariables> block), so we read them with
 * System.getProperty(). This is the standard Java approach — no INI parser needed.
 *
 * All getters provide sensible defaults so tests never throw NullPointerException
 * even if a property is not set.
 */
public class ConfigReader {

    // Prevent instantiation — static access only.
    private ConfigReader() {}

    /**
     * Return the base URL under test.
     *
     * Default: https://www.saucedemo.com
     * Override: -Dbase.url=https://staging.saucedemo.com
     *
     * Python equivalent: config.get("settings", "base_url")
     */
    public static String getBaseUrl() {
        return System.getProperty("base.url", "https://www.saucedemo.com");
    }

    /**
     * Return the standard username for positive login tests.
     * Sauce Demo's "standard_user" is the baseline non-locked user.
     *
     * Kept as a constant here so tests never hardcode "standard_user" inline —
     * if Sauce Demo changes the credential, this is the only file to update.
     */
    public static String getStandardUsername() {
        return System.getProperty("username", "standard_user");
    }

    /**
     * Return the password shared by all Sauce Demo test users.
     */
    public static String getPassword() {
        return System.getProperty("password", "secret_sauce");
    }

    /**
     * Return the locked-out username used in negative login tests.
     */
    public static String getLockedUsername() {
        return "locked_out_user";
    }

    /**
     * Return the default WebDriverWait timeout in seconds.
     * Matches the 10-second default in the Python config.ini.
     */
    public static int getDefaultTimeout() {
        return Integer.parseInt(System.getProperty("timeout", "10"));
    }
}
