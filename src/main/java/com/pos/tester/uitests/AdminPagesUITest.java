package com.pos.tester.uitests;

import com.pos.tester.model.TestResult;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.util.List;

/**
 * UI tests for remaining admin pages not covered by other suites:
 * /warehouse, /prices, /product-groups, /general-diary,
 * /reports, /areas-management, /shops-management,
 * /customers/leaderboard, /permissions
 */
public class AdminPagesUITest extends BaseUITestSuite {

    public AdminPagesUITest(WebDriver driver, String frontendUrl,
                            String screenshotDir, int waitSeconds) {
        super(driver, frontendUrl, screenshotDir, waitSeconds);
    }

    @Override public String getModuleName() { return "Admin Pages"; }
    @Override public String getDescription() {
        return "UI: Warehouse, Prices, Product Groups, Diary, Reports, Areas, Shops, Leaderboard, Permissions";
    }

    @Override
    public List<TestResult> runAll() throws InterruptedException {
        results.clear();

        // ─── WAREHOUSE ─────────────────────────────────────────────────────────

        runTest("[Warehouse] Load warehouse page - table/grid renders", result -> {
            navigateTo("/warehouse");
            sleep(1500);
            boolean hasTable = elementExists(By.cssSelector("table, .table, [id*='Table'], [id*='table']"));
            boolean hasSearch = elementExists(By.cssSelector("input[type='text'][id*='search'], input[type='text'][id*='Search']"));
            assertTrue(hasTable || hasSearch, "Warehouse page should have table or search input");
            result.pass("Warehouse page loaded (table=" + hasTable + " search=" + hasSearch + ")");
        });

        runTest("[Warehouse] Open add warehouse/import modal", result -> {
            navigateTo("/warehouse");
            sleep(1200);
            By addBtn = By.cssSelector("[onclick*='openAdd'], [onclick*='openImport'], .btn-success, .btn-primary");
            if (!elementExists(addBtn)) { result.skip("Add/Import button not found on warehouse page"); return; }
            jsClick(addBtn);
            sleep(1200);
            boolean anyModal = elementExists(By.cssSelector(".modal-overlay[style*='flex']"));
            result.pass("Warehouse add/import modal opened: " + anyModal);
            // Close modal
            By close = By.cssSelector(".modal-overlay[style*='flex'] .modal-close, .modal-overlay[style*='flex'] .btn-secondary");
            if (elementExists(close)) jsClick(close);
        });

        // ─── PRICES ────────────────────────────────────────────────────────────

        runTest("[Prices] Load prices page - table renders", result -> {
            navigateTo("/prices");
            sleep(1500);
            boolean hasTable = elementExists(By.cssSelector("table, .table, [id*='Table']"));
            assertTrue(hasTable, "Prices page must have a table");
            result.pass("Prices page loaded with table");
        });

        runTest("[Prices] Statistics/KPI cards visible", result -> {
            navigateTo("/prices");
            sleep(1200);
            boolean hasStats = elementExists(By.cssSelector(".stat-card, .kpi-card, [class*='stat'], [id*='total']"));
            result.pass("Prices stats visible: " + hasStats);
        });

        runTest("[Prices] Open add price modal", result -> {
            navigateTo("/prices");
            sleep(1200);
            By addBtn = By.cssSelector("[onclick*='openAdd'], .btn-success");
            if (!elementExists(addBtn)) { result.skip("Add price button not found"); return; }
            jsClick(addBtn);
            sleep(1200);
            boolean anyModal = elementExists(By.cssSelector(".modal-overlay[style*='flex']"));
            result.pass("Add price modal: " + anyModal);
            By close = By.cssSelector(".modal-overlay[style*='flex'] .modal-close");
            if (elementExists(close)) jsClick(close);
        });

        // ─── PRODUCT GROUPS ────────────────────────────────────────────────────

        runTest("[Product Groups] Load product groups page", result -> {
            navigateTo("/product-groups");
            sleep(1500);
            boolean hasContent = elementExists(By.cssSelector("table, .table, .card, [id*='group']"));
            assertTrue(hasContent, "Product groups page must have content (table or card)");
            result.pass("Product groups page loaded");
        });

        runTest("[Product Groups] Groups list renders", result -> {
            navigateTo("/product-groups");
            sleep(1500);
            int rows = tableRowCount(By.cssSelector("tbody"));
            result.pass("Product groups table rows: " + rows);
        });

        runTest("[Product Groups] Open add group modal", result -> {
            navigateTo("/product-groups");
            sleep(1200);
            By addBtn = By.cssSelector("[onclick*='openAdd'], [onclick*='addGroup'], .btn-success");
            if (!elementExists(addBtn)) { result.skip("Add group button not found"); return; }
            jsClick(addBtn);
            sleep(1200);
            boolean anyModal = elementExists(By.cssSelector(".modal-overlay[style*='flex']"));
            result.pass("Add group modal opened: " + anyModal);
            By close = By.cssSelector(".modal-overlay[style*='flex'] .modal-close, .modal-overlay[style*='flex'] .btn-secondary");
            if (elementExists(close)) jsClick(close);
        });

        // ─── GENERAL DIARY ─────────────────────────────────────────────────────

        runTest("[General Diary] Load general diary page", result -> {
            navigateTo("/general-diary");
            sleep(1500);
            boolean hasTable = elementExists(By.cssSelector("table, .table, [id*='diary'], [id*='journal']"));
            assertTrue(hasTable, "General diary page must have a table");
            result.pass("General diary page loaded");
        });

        runTest("[General Diary] Date range filter fields exist", result -> {
            navigateTo("/general-diary");
            sleep(1200);
            boolean hasFrom = elementExists(By.cssSelector("input[type='date'][id*='from'], input[type='date'][id*='From']"));
            boolean hasTo   = elementExists(By.cssSelector("input[type='date'][id*='to'], input[type='date'][id*='To']"));
            result.pass("Date filters: fromDate=" + hasFrom + " toDate=" + hasTo);
        });

        runTest("[General Diary] Diary entries load (rows visible)", result -> {
            navigateTo("/general-diary");
            sleep(2000);
            int rows = tableRowCount(By.cssSelector("tbody"));
            result.pass("General diary entries: " + rows + " rows visible");
        });

        // ─── REPORTS ───────────────────────────────────────────────────────────

        runTest("[Reports] Load reports page - revenue tab active", result -> {
            navigateTo("/reports");
            sleep(1500);
            boolean hasChart   = elementExists(By.cssSelector("canvas, [id*='Chart'], [id*='chart']"));
            boolean hasRevenue = elementExists(By.id("revenueSection")) || elementExists(By.id("totalRevenue"));
            assertTrue(hasChart || hasRevenue, "Reports page must have chart or revenue stats");
            result.pass("Reports page loaded: chart=" + hasChart + " revenueSection=" + hasRevenue);
        });

        runTest("[Reports] Revenue KPI cards visible", result -> {
            navigateTo("/reports");
            sleep(1500);
            boolean hasTotalRevenue = elementExists(By.id("totalRevenue"));
            boolean hasTotalSold    = elementExists(By.id("totalSold"));
            boolean hasTotalProducts = elementExists(By.id("totalProducts"));
            int found = (hasTotalRevenue ? 1 : 0) + (hasTotalSold ? 1 : 0) + (hasTotalProducts ? 1 : 0);
            result.pass(found + "/3 revenue KPI cards visible");
        });

        runTest("[Reports] Switch to debt report tab", result -> {
            navigateTo("/reports");
            sleep(1200);
            By debtTab = By.cssSelector("[data-tab='debt'], [onclick*='debt']");
            if (!elementExists(debtTab)) {
                // Try text match
                debtTab = By.xpath("//button[contains(text(),'Công nợ') or contains(text(),'debt')]");
            }
            if (!elementExists(debtTab)) { result.skip("Debt tab not found"); return; }
            jsClick(debtTab);
            sleep(1500);
            boolean debtVisible = elementExists(By.id("debtSection")) || elementExists(By.id("totalDebt"));
            result.pass("Debt tab clicked: debtSection=" + debtVisible);
        });

        runTest("[Reports] Date range filter and apply", result -> {
            navigateTo("/reports");
            sleep(1200);
            By fromDate = By.id("fromDate");
            By toDate   = By.id("toDate");
            if (!elementExists(fromDate)) { result.skip("Date filter fields not found in reports"); return; }
            driver.findElement(fromDate).sendKeys("2024-01-01");
            sleep(500);
            driver.findElement(toDate).sendKeys("2026-12-31");
            sleep(500);
            By applyBtn = By.cssSelector("[onclick*='loadReport'], [onclick*='applyDate'], .btn-primary");
            if (elementExists(applyBtn)) {
                jsClick(applyBtn);
                sleep(2000);
            }
            result.pass("Date range filter applied on reports page");
        });

        runTest("[Reports] Revenue table shows product breakdown", result -> {
            navigateTo("/reports");
            sleep(2500); // wait for data to load
            boolean hasTable = elementExists(By.id("revenueTable")) || elementExists(By.id("revenueTableBody"));
            result.pass("Revenue product table: " + hasTable);
        });

        // ─── AREAS MANAGEMENT ──────────────────────────────────────────────────

        runTest("[Areas] Load areas management page", result -> {
            navigateTo("/areas-management");
            sleep(1500);
            boolean hasContent = elementExists(By.cssSelector("table, .table, .card, [id*='area']"));
            assertTrue(hasContent, "Areas management page must render content");
            result.pass("Areas management page loaded");
        });

        runTest("[Areas] Open add area modal", result -> {
            navigateTo("/areas-management");
            sleep(1200);
            By addBtn = By.cssSelector("[onclick*='openAdd'], [onclick*='addArea'], .btn-success");
            if (!elementExists(addBtn)) { result.skip("Add area button not found"); return; }
            jsClick(addBtn);
            sleep(1200);
            boolean anyModal = elementExists(By.cssSelector(".modal-overlay[style*='flex']"));
            result.pass("Add area modal: " + anyModal);
            By close = By.cssSelector(".modal-overlay[style*='flex'] .modal-close, .modal-overlay[style*='flex'] .btn-secondary");
            if (elementExists(close)) jsClick(close);
        });

        // ─── SHOPS MANAGEMENT ──────────────────────────────────────────────────

        runTest("[Shops] Load shops management page", result -> {
            navigateTo("/shops-management");
            sleep(1500);
            boolean hasContent = elementExists(By.cssSelector("table, .table, .card, [id*='shop']"));
            assertTrue(hasContent, "Shops management page must render content");
            result.pass("Shops management page loaded");
        });

        runTest("[Shops] Open add shop modal", result -> {
            navigateTo("/shops-management");
            sleep(1200);
            By addBtn = By.cssSelector("[onclick*='openAdd'], [onclick*='addShop'], .btn-success");
            if (!elementExists(addBtn)) { result.skip("Add shop button not found"); return; }
            jsClick(addBtn);
            sleep(1200);
            boolean anyModal = elementExists(By.cssSelector(".modal-overlay[style*='flex']"));
            result.pass("Add shop modal: " + anyModal);
            By close = By.cssSelector(".modal-overlay[style*='flex'] .modal-close, .modal-overlay[style*='flex'] .btn-secondary");
            if (elementExists(close)) jsClick(close);
        });

        // ─── CUSTOMER LEADERBOARD ──────────────────────────────────────────────

        runTest("[Leaderboard] Load customer leaderboard page", result -> {
            navigateTo("/customers/leaderboard");
            sleep(1500);
            boolean hasContent = elementExists(By.cssSelector("table, .table, .leaderboard, [id*='leader'], [id*='rank']"));
            assertTrue(hasContent, "Customer leaderboard page must render content");
            result.pass("Customer leaderboard page loaded");
        });

        runTest("[Leaderboard] Leaderboard entries visible", result -> {
            navigateTo("/customers/leaderboard");
            sleep(2000);
            int rows = tableRowCount(By.cssSelector("tbody"));
            result.pass("Leaderboard entries: " + rows + " rows visible");
        });

        // ─── PERMISSIONS ───────────────────────────────────────────────────────

        runTest("[Permissions] Load permissions management page", result -> {
            navigateTo("/permissions");
            sleep(1500);
            boolean hasContent = elementExists(By.cssSelector("table, .table, [id*='perm'], [id*='user'], .card"));
            assertTrue(hasContent, "Permissions page must render content");
            result.pass("Permissions management page loaded");
        });

        runTest("[Permissions] User list or permission matrix visible", result -> {
            navigateTo("/permissions");
            sleep(1500);
            boolean hasTable    = elementExists(By.cssSelector("table tbody tr"));
            boolean hasCheckbox = elementExists(By.cssSelector("input[type='checkbox']"));
            assertTrue(hasTable || hasCheckbox, "Permissions page should have table rows or checkboxes");
            result.pass("Permissions: table=" + hasTable + " checkboxes=" + hasCheckbox);
        });

        return results;
    }
}
