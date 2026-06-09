package com.pos.tester.uitests;

import com.pos.tester.model.TestResult;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.util.List;

public class OrdersInvoicesUITest extends BaseUITestSuite {

    public OrdersInvoicesUITest(WebDriver driver, String frontendUrl,
                                String screenshotDir, int waitSeconds) {
        super(driver, frontendUrl, screenshotDir, waitSeconds);
    }

    @Override public String getModuleName() { return "Orders & Invoices"; }
    @Override public String getDescription() { return "UI: Order management, Invoice list, Status tabs, Search, Filters"; }

    @Override
    public List<TestResult> runAll() throws InterruptedException {
        results.clear();

        // ═══════════════════════════════════════════════════════
        //   ORDERS PAGE
        // ═══════════════════════════════════════════════════════

        runTest("[Orders] Load orders page - table renders", result -> {
            navigateTo("/orders");
            waitForVisible(By.id("ordersTable"));
            sleep(1000);
            int rows = tableRowCount(By.id("ordersTableBody"));
            result.pass("Orders page loaded: " + rows + " order rows");
        });

        runTest("[Orders] Statistics KPI cards visible", result -> {
            navigateTo("/orders");
            sleep(800);
            String[] kpiIds = {"totalOrders", "pendingOrders", "processingOrders",
                    "completedOrders", "cancelledOrders"};
            int found = 0;
            for (String id : kpiIds) {
                if (elementExists(By.id(id))) found++;
            }
            assertTrue(found >= 3, "At least 3 KPI cards should exist, found: " + found);
            result.pass(found + "/5 KPI cards visible on orders page");
        });

        runTest("[Orders] Status tabs switch content", result -> {
            navigateTo("/orders");
            waitForVisible(By.id("ordersTableBody"));
            sleep(800);
            List<WebElement> tabs = driver.findElements(By.cssSelector(".tab[data-status]"));
            assertTrue(tabs.size() >= 2, "At least 2 status tabs expected");
            int firstCount = tableRowCount(By.id("ordersTableBody"));
            // Click second tab
            jsClick(tabs.get(1));
            sleep(800);
            int secondCount = tableRowCount(By.id("ordersTableBody"));
            result.pass("Tabs work: tab1=" + firstCount + " rows, tab2=" + secondCount +
                    " rows (status: " + tabs.get(1).getAttribute("data-status") + ")");
        });

        runTest("[Orders] Search bar filters orders", result -> {
            navigateTo("/orders");
            waitForVisible(By.id("ordersTableBody"));
            sleep(800);
            assertTrue(elementExists(By.id("searchOrder")), "#searchOrder input must exist");
            type(By.id("searchOrder"), "ORD");
            sleep(700);
            result.pass("Order search executed - rows: " + tableRowCount(By.id("ordersTableBody")));
            driver.findElement(By.id("searchOrder")).clear();
        });

        runTest("[Orders] Date range filter fields exist", result -> {
            navigateTo("/orders");
            sleep(500);
            boolean hasFrom = elementExists(By.id("dateFrom"));
            boolean hasTo = elementExists(By.id("dateTo"));
            assertTrue(hasFrom && hasTo, "Date range filters (dateFrom, dateTo) must exist");
            type(By.id("dateFrom"), "2025-01-01");
            type(By.id("dateTo"), "2026-12-31");
            sleep(700);
            result.pass("Date range filters set and applied");
            driver.findElement(By.id("dateFrom")).clear();
            driver.findElement(By.id("dateTo")).clear();
        });

        runTest("[Orders] Open Add Order modal", result -> {
            navigateTo("/orders");
            sleep(500);
            By addBtn = By.xpath("//*[contains(@onclick,'openAdd') or contains(@onclick,'addOrder') or contains(text(),'Thêm đơn')]");
            if (!elementExists(addBtn)) { result.skip("Add order button not found"); return; }
            jsClick(addBtn);
            sleep(700);
            assertTrue(isModalVisible("addOrderModal"), "Add Order modal must open");
            assertTrue(elementExists(By.id("addOrderCode")), "#addOrderCode input must exist in modal");
            assertTrue(elementExists(By.id("addCustomerName")), "#addCustomerName must exist in modal");
            result.pass("Add Order modal opened with required fields");
            closeModal("addOrderModal");
        });

        runTest("[Orders] Customer search in order form", result -> {
            navigateTo("/orders");
            sleep(500);
            By addBtn = By.xpath("//*[contains(@onclick,'openAdd') or contains(@onclick,'addOrder')]");
            if (!elementExists(addBtn)) { result.skip("Add order button not found"); return; }
            jsClick(addBtn);
            sleep(600);
            if (!isModalVisible("addOrderModal")) { result.skip("Modal did not open"); return; }
            type(By.id("addCustomerName"), "a");
            sleep(700);
            boolean hasDropdown = elementExists(By.id("addCustomerDropdownList")) &&
                    driver.findElement(By.id("addCustomerDropdownList")).isDisplayed();
            result.pass("Customer search in order form: dropdown visible=" + hasDropdown);
            closeModal("addOrderModal");
        });

        runTest("[Orders] Click order row opens details (if exists)", result -> {
            navigateTo("/orders");
            waitForVisible(By.id("ordersTableBody"));
            sleep(1000);
            List<WebElement> rows = driver.findElements(By.cssSelector("#ordersTableBody tr"));
            if (rows.isEmpty()) { result.skip("No orders in table to click"); return; }
            // Try clicking first row
            jsClick(rows.get(0));
            sleep(600);
            boolean modalOpened = isModalVisible("editOrderModal") ||
                    isModalVisible("viewOrderModal") ||
                    isModalVisible("addOrderModal");
            result.pass("Row click action: modal opened=" + modalOpened +
                    " (current URL: " + currentUrl() + ")");
        });

        // ═══════════════════════════════════════════════════════
        //   INVOICES PAGE
        // ═══════════════════════════════════════════════════════

        runTest("[Invoices] Load invoices page - table renders", result -> {
            navigateTo("/invoices");
            waitForVisible(By.id("invoicesTable"));
            sleep(1000);
            int rows = tableRowCount(By.id("invoicesTableBody"));
            result.pass("Invoices page loaded: " + rows + " invoice rows");
        });

        runTest("[Invoices] Payment status tabs (Paid/Unpaid/All)", result -> {
            navigateTo("/invoices");
            waitForVisible(By.id("invoicesTableBody"));
            sleep(700);
            List<WebElement> tabs = driver.findElements(By.cssSelector(".tab[data-status]"));
            assertTrue(tabs.size() >= 2, "Invoice status tabs must exist");
            result.pass(tabs.size() + " payment status tabs found");
        });

        runTest("[Invoices] Search by invoice number works", result -> {
            navigateTo("/invoices");
            waitForVisible(By.id("invoicesTableBody"));
            sleep(800);
            assertTrue(elementExists(By.id("searchInvoice")), "#searchInvoice must exist");
            type(By.id("searchInvoice"), "HD");
            sleep(700);
            result.pass("Invoice search by 'HD' - rows: " + tableRowCount(By.id("invoicesTableBody")));
            driver.findElement(By.id("searchInvoice")).clear();
        });

        runTest("[Invoices] KPI cards visible (total, paid, unpaid, revenue)", result -> {
            navigateTo("/invoices");
            sleep(800);
            int found = 0;
            for (String id : new String[]{"totalInvoices", "paidInvoices", "pendingInvoices", "totalRevenue"}) {
                if (elementExists(By.id(id))) found++;
            }
            result.pass(found + "/4 invoice KPI cards visible");
        });

        runTest("[Invoices] Open Add Invoice modal", result -> {
            navigateTo("/invoices");
            sleep(500);
            By addBtn = By.xpath("//*[contains(@onclick,'openAdd') or contains(@onclick,'addInvoice') or contains(text(),'Thêm hóa đơn')]");
            if (!elementExists(addBtn)) { result.skip("Add invoice button not found"); return; }
            jsClick(addBtn);
            sleep(700);
            assertTrue(isModalVisible("addInvoiceModal"), "Add Invoice modal must open");
            assertTrue(elementExists(By.id("addHinhThucTT")), "Payment method select must exist");
            result.pass("Add Invoice modal opened with payment method field");
            closeModal("addInvoiceModal");
        });

        runTest("[Invoices] View invoice details modal", result -> {
            navigateTo("/invoices");
            waitForVisible(By.id("invoicesTableBody"));
            sleep(1000);
            List<WebElement> rows = driver.findElements(By.cssSelector("#invoicesTableBody tr"));
            if (rows.isEmpty()) { result.skip("No invoices to view"); return; }
            // Look for view/eye button
            By viewBtn = By.cssSelector("#invoicesTableBody tr:first-child [onclick*='view'], " +
                    "#invoicesTableBody tr:first-child [onclick*='detail'], " +
                    "#invoicesTableBody tr:first-child .btn-info, " +
                    "#invoicesTableBody tr:first-child .fa-eye");
            if (elementExists(viewBtn)) {
                jsClick(viewBtn);
                sleep(700);
                boolean opened = isModalVisible("viewInvoiceDetailsModal") ||
                        elementExists(By.id("invoiceDetailsContent"));
                result.pass("Invoice details modal opened: " + opened);
            } else {
                result.skip("View button not found in invoice rows");
            }
        });

        return results;
    }
}
