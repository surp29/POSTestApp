package com.pos.tester.uitests;

import com.pos.tester.model.TestResult;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public abstract class BaseUITestSuite {
    protected WebDriver driver;
    protected final String frontendUrl;
    protected final String screenshotDir;
    protected final int waitSeconds;
    protected Consumer<TestResult> onTestComplete;
    protected java.util.List<TestResult> results = new java.util.ArrayList<>();

    public BaseUITestSuite(WebDriver driver, String frontendUrl, String screenshotDir, int waitSeconds) {
        this.driver = driver;
        this.frontendUrl = frontendUrl;
        this.screenshotDir = screenshotDir;
        this.waitSeconds = waitSeconds;
    }

    public void setOnTestComplete(Consumer<TestResult> callback) {
        this.onTestComplete = callback;
    }

    public abstract String getModuleName();
    public abstract String getDescription();
    public abstract List<TestResult> runAll() throws InterruptedException;

    public List<TestResult> getResults() { return results; }
    public long passCount() { return results.stream().filter(r -> r.getStatus() == TestResult.Status.PASS).count(); }
    public long failCount() { return results.stream().filter(r -> r.getStatus() == TestResult.Status.FAIL).count(); }

    // ─── Test execution ──────────────────────────────────────────────────────

    @FunctionalInterface
    public interface UITestAction {
        void execute(TestResult result) throws Exception;
    }

    protected TestResult runTest(String testName, UITestAction action) {
        TestResult result = new TestResult("[UI] " + getModuleName(), testName);
        try {
            action.execute(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.fail("Test interrupted");
        } catch (TimeoutException e) {
            takeScreenshot(testName);
            result.fail("Timeout waiting for element", e.getMessage());
        } catch (NoSuchElementException e) {
            takeScreenshot(testName);
            result.fail("Element not found: " + e.getMessage());
        } catch (AssertionError e) {
            takeScreenshot(testName);
            result.fail("Assertion failed: " + e.getMessage());
        } catch (Exception e) {
            takeScreenshot(testName);
            result.fail("Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        results.add(result);
        if (onTestComplete != null) onTestComplete.accept(result);
        return result;
    }

    // ─── Navigation ──────────────────────────────────────────────────────────

    protected void navigateTo(String path) {
        driver.get(frontendUrl + path);
        waitForPageLoad();
        sleep(1200); // visible pause so user can see the page loaded
    }

    protected void waitForPageLoad() {
        new WebDriverWait(driver, Duration.ofSeconds(waitSeconds))
                .until(d -> ((JavascriptExecutor) d)
                        .executeScript("return document.readyState").equals("complete"));
    }

    // ─── Wait helpers ─────────────────────────────────────────────────────────

    protected WebElement waitForElement(By by) {
        return new WebDriverWait(driver, Duration.ofSeconds(waitSeconds))
                .until(ExpectedConditions.presenceOfElementLocated(by));
    }

    protected WebElement waitForVisible(By by) {
        return new WebDriverWait(driver, Duration.ofSeconds(waitSeconds))
                .until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    protected WebElement waitForClickable(By by) {
        return new WebDriverWait(driver, Duration.ofSeconds(waitSeconds))
                .until(ExpectedConditions.elementToBeClickable(by));
    }

    protected boolean waitForVisibleSafe(By by) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOfElementLocated(by));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    protected boolean waitForInvisibleSafe(By by) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(waitSeconds))
                    .until(ExpectedConditions.invisibilityOfElementLocated(by));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    protected boolean elementExists(By by) {
        return !driver.findElements(by).isEmpty();
    }

    protected void waitForTextPresent(By by, String text) {
        new WebDriverWait(driver, Duration.ofSeconds(waitSeconds))
                .until(ExpectedConditions.textToBePresentInElementLocated(by, text));
    }

    // ─── Interaction helpers ──────────────────────────────────────────────────

    protected void click(By by) {
        waitForClickable(by).click();
    }

    protected void type(By by, String text) {
        WebElement el = waitForVisible(by);
        el.clear();
        el.sendKeys(text);
    }

    protected void typeInto(WebElement el, String text) {
        el.clear();
        el.sendKeys(text);
    }

    protected void selectByValue(By by, String value) {
        new Select(waitForVisible(by)).selectByValue(value);
    }

    protected void selectByText(By by, String text) {
        new Select(waitForVisible(by)).selectByVisibleText(text);
    }

    protected String getText(By by) {
        return waitForVisible(by).getText().trim();
    }

    protected String getValue(By by) {
        return waitForElement(by).getAttribute("value");
    }

    protected void clearAndType(By by, String text) {
        WebElement el = waitForClickable(by);
        el.sendKeys(Keys.CONTROL + "a");
        el.sendKeys(Keys.DELETE);
        el.sendKeys(text);
    }

    protected void jsClick(By by) {
        WebElement el = waitForElement(by);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        sleep(1000); // visible pause so user can see button was clicked
    }

    protected void jsClick(WebElement el) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        sleep(1000);
    }

    protected void scrollIntoView(WebElement el) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", el);
    }

    protected void hover(By by) {
        new Actions(driver).moveToElement(waitForElement(by)).perform();
    }

    protected void pressKey(By by, Keys key) {
        waitForElement(by).sendKeys(key);
    }

    // ─── URL helpers ─────────────────────────────────────────────────────────

    protected String currentUrl() {
        return driver.getCurrentUrl();
    }

    protected boolean urlContains(String part) {
        return driver.getCurrentUrl().contains(part);
    }

    protected void waitForUrl(String partialUrl) {
        new WebDriverWait(driver, Duration.ofSeconds(waitSeconds))
                .until(ExpectedConditions.urlContains(partialUrl));
    }

    // ─── Table helpers ────────────────────────────────────────────────────────

    protected int tableRowCount(By tableBodyBy) {
        try {
            WebElement tbody = waitForElement(tableBodyBy);
            return tbody.findElements(By.tagName("tr")).size();
        } catch (Exception e) {
            return 0;
        }
    }

    protected boolean tableContainsText(By tableBodyBy, String text) {
        try {
            WebElement tbody = waitForElement(tableBodyBy);
            return tbody.getText().contains(text);
        } catch (Exception e) {
            return false;
        }
    }

    // ─── Modal helpers ────────────────────────────────────────────────────────

    protected void openModal(String modalId, By triggerBy) {
        click(triggerBy);
        waitForVisible(By.id(modalId));
    }

    protected void closeModal(String modalId) {
        driver.findElements(By.cssSelector("#" + modalId + " .modal-close"))
                .stream().findFirst()
                .ifPresent(btn -> {
                    try { btn.click(); } catch (Exception ignored) { jsClick(btn); }
                });
        waitForInvisibleSafe(By.id(modalId));
    }

    protected boolean isModalVisible(String modalId) {
        return elementExists(By.id(modalId)) &&
               driver.findElement(By.id(modalId)).isDisplayed();
    }

    // ─── Statistics helpers ────────────────────────────────────────────────────

    protected int getStatCount(String elementId) {
        try {
            String text = driver.findElement(By.id(elementId)).getText().trim().replace(",", "");
            return Integer.parseInt(text);
        } catch (Exception e) {
            return -1;
        }
    }

    // ─── Assertions ───────────────────────────────────────────────────────────

    protected void assertTrue(boolean condition, String msg) {
        if (!condition) throw new AssertionError(msg);
    }

    protected void assertContains(String haystack, String needle) {
        if (haystack == null || !haystack.contains(needle))
            throw new AssertionError("Expected to contain '" + needle + "' in: " + haystack);
    }

    protected void assertNotNull(Object v, String name) {
        if (v == null) throw new AssertionError(name + " must not be null");
    }

    // ─── Screenshot ───────────────────────────────────────────────────────────

    protected String takeScreenshot(String testName) {
        if (driver == null) return null;
        try {
            File dir = new File(screenshotDir);
            if (!dir.exists()) dir.mkdirs();
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String safeName = testName.replaceAll("[^a-zA-Z0-9_]", "_");
            File dest = new File(dir, safeName + "_" + ts + ".png");
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return dest.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    // ─── Short sleep (use sparingly, prefer explicit waits) ──────────────────

    protected void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
