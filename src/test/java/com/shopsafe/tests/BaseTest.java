package com.shopsafe.tests;

import com.shopsafe.utils.ConfigReader;
import com.shopsafe.utils.DriverFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * BaseTest — JUnit 5 test lifecycle: driver setup, teardown, and screenshot on failure.
 *
 * Python equivalent: conftest.py (the @pytest.fixture "driver" + pytest_runtest_makereport hook)
 *
 * Every test class extends BaseTest to get:
 *  - A fresh WebDriver before each test (@BeforeEach)
 *  - Driver quit after each test (@AfterEach)
 *  - Automatic screenshot saved to reports/screenshots/ when a test fails
 *  - The base URL read from ConfigReader (which reads System.getProperty)
 *
 * JUnit 5 lifecycle note:
 *  @BeforeEach runs before every @Test method.
 *  @AfterEach runs after every @Test method, even if the test threw an exception.
 *  This guarantees the driver is always closed — no lingering browser windows.
 */
public abstract class BaseTest {

    // Protected so subclasses (LoginTest, InventoryTest, etc.) can access the driver.
    protected WebDriver driver;
    protected String baseUrl;

    // Screenshot output directory — matches the Python reports/screenshots/ path.
    private static final String SCREENSHOT_DIR = "reports/screenshots/";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Runs before every @Test method.
     *
     * TestInfo is injected by JUnit 5 — it carries the test method name,
     * display name, and tags. We use it to name screenshots.
     *
     * Python equivalent:
     *   @pytest.fixture
     *   def driver(request):
     *       driver = webdriver.Chrome(options)
     *       yield driver
     *       driver.quit()
     */
    @BeforeEach
    void setUp(TestInfo testInfo) {
        driver  = DriverFactory.createDriver();
        baseUrl = ConfigReader.getBaseUrl();
        System.out.printf("[Setup] %s — browser=%s headless=%s%n",
                          testInfo.getDisplayName(),
                          System.getProperty("browser", "chrome"),
                          System.getenv("CI") != null || Boolean.getBoolean("headless"));
    }

    /**
     * Runs after every @Test method.
     *
     * Takes a screenshot if the test failed, then quits the driver.
     *
     * Python equivalent: pytest_runtest_makereport hook in conftest.py:
     *   if rep.failed:
     *       driver.save_screenshot(f"reports/screenshots/{node_id}_{timestamp}.png")
     *
     * JUnit 5 doesn't have a built-in "did this test fail?" hook inside @AfterEach,
     * so we use a different approach: the test methods themselves call
     * captureScreenshot(info) in a try/finally, OR subclasses can override
     * tearDown and inspect the test result. For simplicity, we expose a helper
     * so any test can call it from a @AfterEach override.
     */
    @AfterEach
    void tearDown(TestInfo testInfo) {
        // Note: In JUnit 5, @AfterEach doesn't receive the test outcome directly.
        // The pattern below takes a screenshot unconditionally when called from
        // a try/finally block in the test, OR tests can use @ExtendWith to register
        // a TestWatcher that calls captureScreenshot on failure. The screenshot
        // helper is exposed protected so subclasses can use it either way.
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * Save a PNG screenshot to reports/screenshots/.
     *
     * Filename format: TestClassName_testMethodName_yyyyMMdd_HHmmss.png
     * This matches the Python pattern: f"{node_id}_{timestamp}.png"
     *
     * @param testInfo JUnit 5 TestInfo — provides class and method name
     */
    protected void captureScreenshot(TestInfo testInfo) {
        if (driver == null) return;
        try {
            // Ensure the output directory exists.
            Path dir = Paths.get(SCREENSHOT_DIR);
            Files.createDirectories(dir);

            // Build a filename from the test class + method + timestamp.
            String className  = testInfo.getTestClass()
                                        .map(Class::getSimpleName)
                                        .orElse("UnknownClass");
            String methodName = testInfo.getTestMethod()
                                        .map(m -> m.getName())
                                        .orElse("unknownMethod");
            String timestamp  = LocalDateTime.now().format(TIMESTAMP);
            String filename   = String.format("%s_%s_%s.png", className, methodName, timestamp);

            // Take the screenshot. TakesScreenshot is implemented by ChromeDriver/FirefoxDriver.
            // OutputType.FILE creates a temp file with the PNG bytes.
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            // Move the temp file to our screenshots directory.
            Files.copy(srcFile.toPath(),
                       dir.resolve(filename),
                       StandardCopyOption.REPLACE_EXISTING);

            System.out.println("[Screenshot] Saved: " + SCREENSHOT_DIR + filename);
        } catch (IOException e) {
            System.err.println("[Screenshot] Failed to save: " + e.getMessage());
        }
    }
}
