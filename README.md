# shopsafe-java

[![CI](https://github.com/InderParmar/shopsafe-java/actions/workflows/ci.yml/badge.svg)](https://github.com/InderParmar/shopsafe-java/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/java-17-blue?logo=openjdk&logoColor=white)
![Selenium](https://img.shields.io/badge/selenium-4.x-green?logo=selenium&logoColor=white)
![JUnit 5](https://img.shields.io/badge/tested%20with-JUnit%205-orange)
![Maven](https://img.shields.io/badge/build-Maven-red?logo=apachemaven&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

> **Java + Selenium + JUnit 5 + Maven** port of the ShopSafe e-commerce test suite —
> validating saucedemo.com across **36 test cases**, **5 modules**, and **2 browsers**,
> with full CI/CD via GitHub Actions.
>
> Converted from the [Python/pytest original](https://github.com/InderParmar/selenium-ecommerce-qa-suite)
> using the identical Page Object Model architecture — demonstrating the same framework
> patterns in the language stack used in enterprise QA (Veeva, Workday, SAP).

---

## What this project covers

| Module | Test Cases | Coverage |
|---|---|---|
| Login | 9 | Valid login, locked-out user, wrong password, empty fields, error dismissal, data-driven via JSON |
| Inventory | 9 | Page state, all 4 sort orders with full-list assertions, cart badge management |
| Cart | 6 | Item display, count validation, scoped price consistency, remove item, navigation |
| Checkout | 9 | Step 1 form validation, step 2 float math assertions (subtotal + tax = total), order confirmation |
| End-to-End | 3 | Full purchase flow, data-driven checkout with success and error paths |
| **Total** | **36+** | |

---

## Framework architecture

```
shopsafe-java/
│
├── src/main/java/com/shopsafe/
│   ├── pages/
│   │   ├── BasePage.java          # Shared Selenium helpers — click, type, getText, scoped search
│   │   ├── LoginPage.java         # Login flow, error handling, error dismissal
│   │   ├── InventoryPage.java     # Sort validation, cart badge management, page chaining
│   │   ├── CartPage.java          # Item validation, scoped price check, checkout navigation
│   │   └── CheckoutPage.java      # Form validation, float math, order confirmation
│   └── utils/
│       ├── DriverFactory.java     # Chrome/Firefox creation, headless detection, CI auto-detect
│       ├── ConfigReader.java      # Reads system properties — base URL, credentials, timeouts
│       └── TestDataReader.java    # Jackson JSON loader for parametrized test data
│
├── src/test/java/com/shopsafe/tests/
│   ├── BaseTest.java              # JUnit 5 @BeforeEach/@AfterEach, screenshot on failure
│   ├── LoginTest.java
│   ├── InventoryTest.java
│   ├── CartTest.java
│   ├── CheckoutTest.java
│   └── E2ETest.java
│
├── test-data/                     # External JSON — decoupled from test logic
│   ├── login_data.json
│   └── checkout_data.json
│
├── .github/workflows/
│   └── ci.yml                     # Chrome + Firefox parallel matrix, artifact upload
│
└── pom.xml                        # Dependencies, Surefire config, property defaults
```

---

## Key technical decisions

**Explicit By locators — not PageFactory `@FindBy`.**
PageFactory returns a proxy element that does *not* wait — it throws `StaleElementReferenceException` immediately if the element isn't present. All locators are `private static final By` constants paired with `WebDriverWait`, so every interaction waits correctly and timeouts centralise in `BasePage`.

**Zero Selenium in the test layer.**
Test classes contain no `By`, `WebDriverWait`, or `driver.findElement()` calls. Tests call page object methods and make assertions — nothing else. This is the pattern that makes POM frameworks maintainable at scale.

**Logic-based sort assertions — no hardcoded product names.**
```java
// After selecting A→Z sort:
List<String> actual   = inventoryPage.getAllProductNames();   // live DOM order
List<String> expected = inventoryPage.getSortedNamesAZ();    // sorted copy
assertEquals(expected, actual);
// Works for 6 products or 600. Never needs updating.
```

**Scoped DOM traversal for price consistency.**
`CartPage.getPriceByName()` iterates cart rows and reads the price from the *same container* as the matching name — guaranteeing name and price belong to the same product, regardless of DOM rendering order.

**Float math assertion with tolerance.**
```java
double itemTotal = checkoutPage.getItemTotal();
double tax       = checkoutPage.getTax();
double total     = checkoutPage.getTotal();
assertEquals(itemTotal + tax, total, 0.01);
// delta 0.01: catches real errors while ignoring IEEE 754 floating-point noise
```

**Auto-headless in CI.**
`DriverFactory` detects `System.getenv("CI") != null` — GitHub Actions sets this automatically. Local runs stay headed for debugging. No flag switching required.

**Page chaining pattern.**
```java
// Tests read as user stories, not automation scripts:
CheckoutPage checkout = new LoginPage(driver)
    .open(baseUrl)
    .login("standard_user", "secret_sauce")   // → InventoryPage
    .addToCartByName("Sauce Labs Backpack")
    .goToCart()                                // → CartPage
    .proceedToCheckout();                      // → CheckoutPage
```

---

## Quick start

```bash
# Clone the repo
git clone https://github.com/InderParmar/shopsafe-java.git
cd shopsafe-java

# Run the full test suite (Chrome, headed)
mvn test

# Run on Firefox
mvn test -Dbrowser=firefox

# Run headless
mvn test -Dheadless=true

# Run a specific test class
mvn test -Dtest=LoginTest

# Run against a different URL
mvn test -Dbase.url=https://staging.saucedemo.com
```

Requirements: Java 17+, Maven 3.8+. No manual driver setup — WebDriverManager handles it.

---

## CI/CD pipeline

Every push to `main` triggers the full suite on Chrome and Firefox in parallel.

```
push to main
    │
    ├── job: test (chrome)           ├── job: test (firefox)
    │   ├── setup Java 17            │   ├── setup Java 17
    │   ├── restore Maven cache      │   ├── restore Maven cache
    │   ├── setup Chrome             │   ├── setup Firefox
    │   ├── mvn test -Dbrowser=chrome│   ├── mvn test -Dbrowser=firefox
    │   ├── upload Surefire report   │   ├── upload Surefire report
    │   └── upload screenshots       │   └── upload screenshots
```

- `fail-fast: false` — both browsers always complete even if one fails
- Surefire XML reports uploaded as downloadable artifacts after every run
- Failure screenshots auto-captured and uploaded
- Artifacts retained 30 days per run

---

## Python → Java translation notes

This project is a direct port of the [Python/pytest version](https://github.com/InderParmar/selenium-ecommerce-qa-suite). Key translation decisions:

| Python | Java equivalent |
|---|---|
| `conftest.py` `@pytest.fixture` | `BaseTest.java` `@BeforeEach` / `@AfterEach` |
| `@pytest.mark.parametrize` | `@ParameterizedTest` + `@MethodSource` |
| `WebDriverWait` in `wait_helper.py` | `WebDriverWait` centralised in `BasePage.java` |
| `config.ini` + `configparser` | `System.getProperty()` + Maven `-D` flags |
| `json.load()` in `data_reader.py` | Jackson `ObjectMapper.readValue()` |
| `webdriver-manager` (Python lib) | `WebDriverManager` (bonigarcia Java lib) |
| `pytest_runtest_makereport` hook | `BaseTest.captureScreenshot()` helper |
| `os.environ.get("CI")` | `System.getenv("CI")` |

---

## Tech stack

| Tool | Version | Purpose |
|---|---|---|
| Java | 17 (LTS) | Core language |
| Selenium WebDriver | 4.x | Browser automation |
| JUnit 5 (Jupiter) | 5.10 | Test runner, parametrized tests |
| WebDriverManager | 5.x | Automatic driver binary management |
| Jackson Databind | 2.17 | JSON test data deserialization |
| Maven | 3.8+ | Build, dependency management, Surefire runner |
| GitHub Actions | — | CI/CD pipeline |

---

## Author

**Inderpreet Singh Parmar**
QA Automation Engineer · Toronto, ON
[LinkedIn](https://ca.linkedin.com/in/inderpreet-singh-parmar-7abb23230) · [Portfolio](https://inder-website.vercel.app) · [GitHub](https://github.com/InderParmar)
