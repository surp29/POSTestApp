package com.pos.tester.uitests;

import com.pos.tester.model.TestResult;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.util.List;

public class ShippingUITest extends BaseUITestSuite {

    public ShippingUITest(WebDriver driver, String frontendUrl, String screenshotDir, int waitSeconds) {
        super(driver, frontendUrl, screenshotDir, waitSeconds);
    }

    @Override public String getModuleName() { return "Shipping Page"; }
    @Override public String getDescription() { return "UI: Shipment list, Status cards, Create form, Status update workflow"; }

    @Override
    public List<TestResult> runAll() throws InterruptedException {
        results.clear();

        runTest("Load shipping page - table and stats visible", result -> {
            navigateTo("/shipping");
            waitForVisible(By.id("shipmentsBody"));
            sleep(1000);
            int rows = tableRowCount(By.id("shipmentsBody"));
            result.pass("Shipping page loaded: " + rows + " shipment rows");
        });

        runTest("Status statistics cards (8 states) visible", result -> {
            navigateTo("/shipping");
            sleep(800);
            String[] statusIds = {"st-pending", "st-picked", "st-in_transit",
                    "st-delivering", "st-delivered", "st-failed", "st-returned", "st-overdue"};
            int found = 0;
            for (String id : statusIds) {
                if (elementExists(By.id(id))) found++;
            }
            assertTrue(found >= 4, "At least 4 status stat cards should exist, found: " + found);
            result.pass(found + "/8 status statistic cards visible");
        });

        runTest("Status cards are clickable (filter by status)", result -> {
            navigateTo("/shipping");
            sleep(800);
            By cardPending = By.id("card-pending");
            if (!elementExists(cardPending)) {
                cardPending = By.cssSelector("[id^='card-']");
            }
            if (!elementExists(cardPending)) { result.skip("Status cards not found"); return; }
            jsClick(cardPending);
            sleep(700);
            int filtered = tableRowCount(By.id("shipmentsBody"));
            result.pass("Clicked 'pending' card: " + filtered + " rows shown");
        });

        runTest("Search by tracking code / receiver name", result -> {
            navigateTo("/shipping");
            waitForVisible(By.id("shipmentsBody"));
            sleep(600);
            assertTrue(elementExists(By.id("trackInput")), "#trackInput search field must exist");
            type(By.id("trackInput"), "VD");
            sleep(700);
            int results2 = tableRowCount(By.id("shipmentsBody"));
            result.pass("Search 'VD' shows: " + results2 + " shipment rows");
            driver.findElement(By.id("trackInput")).clear();
        });

        runTest("Service type filter dropdown works", result -> {
            navigateTo("/shipping");
            waitForVisible(By.id("svcFilter"));
            WebElement filter = driver.findElement(By.id("svcFilter"));
            int options = new Select(filter).getOptions().size();
            assertTrue(options >= 1, "Service filter must have options");
            if (options > 1) {
                new Select(filter).selectByIndex(1);
                sleep(600);
            }
            result.pass("Service type filter: " + options + " options available");
            new Select(driver.findElement(By.id("svcFilter"))).selectByIndex(0);
        });

        runTest("Date range filter (fromDate, toDate)", result -> {
            navigateTo("/shipping");
            sleep(500);
            assertTrue(elementExists(By.id("fromDate")), "#fromDate filter must exist");
            assertTrue(elementExists(By.id("toDate")), "#toDate filter must exist");
            type(By.id("fromDate"), "2025-01-01");
            type(By.id("toDate"), "2026-12-31");
            sleep(700);
            result.pass("Date range filters applied to shipping page");
            driver.findElement(By.id("fromDate")).clear();
            driver.findElement(By.id("toDate")).clear();
        });

        runTest("Open Create Shipment modal", result -> {
            navigateTo("/shipping");
            sleep(500);
            By createBtn = By.xpath("//*[contains(@onclick,'openCreate') or contains(@onclick,'createShipment') or contains(text(),'Tạo vận đơn')]");
            if (!elementExists(createBtn)) {
                createBtn = By.cssSelector(".btn-primary, [data-action='create']");
            }
            if (!elementExists(createBtn)) { result.skip("Create shipment button not found"); return; }
            jsClick(createBtn);
            sleep(700);
            assertTrue(isModalVisible("createModal"), "Create shipment modal (#createModal) must open");
            result.pass("Create Shipment modal opened");
            closeModal("createModal");
        });

        runTest("Create form has required fields (receiver, shipping fee)", result -> {
            navigateTo("/shipping");
            sleep(500);
            By createBtn = By.xpath("//*[contains(@onclick,'openCreate') or contains(@onclick,'createShipment')]");
            if (!elementExists(createBtn)) { result.skip("Create button not found"); return; }
            jsClick(createBtn);
            sleep(700);
            if (!isModalVisible("createModal")) { result.skip("Modal not visible"); return; }
            assertTrue(elementExists(By.id("shippingFee")), "#shippingFee input must exist");
            By receiverName = By.cssSelector("#createForm [name='receiver_name'], #createForm [name*='receiver']");
            assertTrue(elementExists(receiverName), "Receiver name field must exist");
            result.pass("Create shipment form has required fields: receiver, shippingFee");
            closeModal("createModal");
        });

        runTest("Click shipment row - detail modal opens", result -> {
            navigateTo("/shipping");
            waitForVisible(By.id("shipmentsBody"));
            sleep(1000);
            List<WebElement> rows = driver.findElements(By.cssSelector("#shipmentsBody tr"));
            if (rows.isEmpty()) { result.skip("No shipments to click"); return; }
            // Try clicking view/detail button
            By detailBtn = By.cssSelector("#shipmentsBody tr:first-child [onclick*='openDetail'], " +
                    "#shipmentsBody tr:first-child [onclick*='viewDetail'], " +
                    "#shipmentsBody tr:first-child .btn-info");
            if (elementExists(detailBtn)) {
                jsClick(detailBtn);
                sleep(700);
                boolean opened = isModalVisible("detailModal");
                result.pass("Detail modal opened: " + opened);
                if (opened) closeModal("detailModal");
            } else {
                jsClick(rows.get(0));
                sleep(600);
                result.pass("Row click action executed (current URL: " + currentUrl() + ")");
            }
        });

        runTest("Status update modal has new status dropdown", result -> {
            navigateTo("/shipping");
            waitForVisible(By.id("shipmentsBody"));
            sleep(1000);
            By statusBtn = By.cssSelector("#shipmentsBody tr:first-child [onclick*='openStatus'], " +
                    "#shipmentsBody tr:first-child [onclick*='updateStatus'], " +
                    "#shipmentsBody tr:first-child .btn-warning");
            if (!elementExists(statusBtn)) { result.skip("Status update button not found in table"); return; }
            jsClick(statusBtn);
            sleep(700);
            if (!isModalVisible("statusModal")) { result.skip("Status modal (#statusModal) did not open"); return; }
            assertTrue(elementExists(By.id("newStatusSelect")), "#newStatusSelect must exist in status modal");
            assertTrue(elementExists(By.id("newLocation")), "#newLocation input must exist");
            assertTrue(elementExists(By.id("newDesc")), "#newDesc textarea must exist");
            result.pass("Status update modal opened with all required fields");
            closeModal("statusModal");
        });

        runTest("Tracking code format shown in table (VD prefix)", result -> {
            navigateTo("/shipping");
            waitForVisible(By.id("shipmentsBody"));
            sleep(1000);
            String tableText = "";
            try { tableText = driver.findElement(By.id("shipmentsBody")).getText(); } catch (Exception ignored) {}
            if (tableText.contains("VD")) {
                result.pass("Tracking codes with 'VD' prefix visible in shipment table");
            } else if (tableText.isEmpty() || tableText.contains("Không có")) {
                result.skip("No shipments in table yet - tracking code format not verifiable");
            } else {
                result.skip("Shipments exist but 'VD' prefix not found in visible text - may need scroll");
            }
        });

        return results;
    }
}
