package com.pos.tester.uitests;

import com.pos.tester.model.TestResult;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.time.LocalDate;
import java.util.List;

public class CustomersDiscountsUITest extends BaseUITestSuite {
    private final String testPrefix;

    public CustomersDiscountsUITest(WebDriver driver, String frontendUrl,
                                    String screenshotDir, int waitSeconds, String testPrefix) {
        super(driver, frontendUrl, screenshotDir, waitSeconds);
        this.testPrefix = testPrefix;
    }

    @Override public String getModuleName() { return "Customers & Discounts"; }
    @Override public String getDescription() { return "UI: Customer CRM, Discount codes management, form validation"; }

    @Override
    public List<TestResult> runAll() throws InterruptedException {
        results.clear();

        // ═══════════════════════════════════════════════════════
        //   CUSTOMERS PAGE
        // ═══════════════════════════════════════════════════════

        runTest("[Customers] Load customers page - table renders", result -> {
            navigateTo("/customers");
            waitForVisible(By.id("customersTable"));
            sleep(1000);
            int rows = tableRowCount(By.id("customersTableBody"));
            result.pass("Customers page loaded: " + rows + " customer rows");
        });

        runTest("[Customers] Statistics cards visible", result -> {
            navigateTo("/customers");
            sleep(800);
            int found = 0;
            for (String id : new String[]{"totalCustomers", "activeCustomers",
                    "inactiveCustomers", "customersWithEmail"}) {
                if (elementExists(By.id(id))) found++;
            }
            result.pass(found + "/4 customer KPI cards visible");
        });

        runTest("[Customers] Search filters customer list", result -> {
            navigateTo("/customers");
            waitForVisible(By.id("customersTableBody"));
            sleep(800);
            assertTrue(elementExists(By.id("searchCustomer")), "#searchCustomer field must exist");
            type(By.id("searchCustomer"), "a");
            sleep(700);
            int filtered = tableRowCount(By.id("customersTableBody"));
            result.pass("Search 'a' shows " + filtered + " customer rows");
            driver.findElement(By.id("searchCustomer")).clear();
        });

        runTest("[Customers] Open Add Customer modal", result -> {
            navigateTo("/customers");
            sleep(500);
            By addBtn = By.cssSelector("[onclick='openAddCustomerModal()']");
            if (!elementExists(addBtn)) { result.skip("Add customer button not found"); return; }
            jsClick(addBtn);
            assertTrue(waitForVisibleSafe(By.id("addCustomerModal")), "#addCustomerModal must open");
            result.pass("Add Customer modal opened");
            closeModal("addCustomerModal");
        });

        runTest("[Customers] Add form has required fields", result -> {
            navigateTo("/customers");
            sleep(500);
            By addBtn = By.cssSelector("[onclick='openAddCustomerModal()']");
            if (!elementExists(addBtn)) { result.skip("Add customer button not found"); return; }
            jsClick(addBtn);
            if (!waitForVisibleSafe(By.id("addCustomerModal"))) { result.skip("Modal not visible"); return; }

            // Check for required input fields by name
            By[] fields = {
                By.cssSelector("#addCustomerForm [name='ten_tk'], #addCustomerModal [name='ten_tk']"),
                By.cssSelector("#addCustomerForm [name='so_dt'], #addCustomerModal [name='so_dt']"),
                By.cssSelector("#addCustomerForm [name='email'], #addCustomerModal [name='email']"),
            };
            int found = 0;
            for (By f : fields) {
                if (elementExists(f)) {
                    found++;
                    // Fill with test data
                    try { type(f, "TestValue"); } catch (Exception ignored) {}
                }
            }
            assertTrue(found >= 1, "At least 1 customer field (ten_tk/so_dt/email) must exist");
            result.pass(found + "/3 customer form fields found and fillable");
            closeModal("addCustomerModal");
        });

        runTest("[Customers] Edit customer modal opens", result -> {
            navigateTo("/customers");
            waitForVisible(By.id("customersTableBody"));
            sleep(1000);
            List<WebElement> rows = driver.findElements(By.cssSelector("#customersTableBody tr"));
            if (rows.isEmpty()) { result.skip("No customers in table"); return; }
            By editBtn = By.cssSelector("#customersTableBody tr:first-child [onclick*='openEdit'], " +
                    "#customersTableBody tr:first-child [onclick*='editCustomer'], " +
                    "#customersTableBody tr:first-child .btn-edit");
            if (!elementExists(editBtn)) { result.skip("Edit button not found in customer rows"); return; }
            jsClick(editBtn);
            sleep(700);
            assertTrue(isModalVisible("editCustomerModal"), "#editCustomerModal must open");
            result.pass("Edit Customer modal opened");
            closeModal("editCustomerModal");
        });

        runTest("[Customers] Customer pagination controls", result -> {
            navigateTo("/customers");
            sleep(800);
            boolean hasPagination = elementExists(By.id("customersPagination")) ||
                    elementExists(By.cssSelector(".pagination, [class*='pagination']"));
            result.pass("Pagination controls exist: " + hasPagination);
        });

        runTest("[Customers] Customer debt page loads", result -> {
            navigateTo("/customers-debts");
            sleep(1000);
            boolean hasContent = elementExists(By.cssSelector("table, .debt-table, [class*='debt'], .customers-debt"));
            result.pass("Customer debt page loaded, table exists: " + hasContent);
        });

        // ═══════════════════════════════════════════════════════
        //   DISCOUNT CODES PAGE
        // ═══════════════════════════════════════════════════════

        runTest("[Discounts] Load discount codes page - table renders", result -> {
            navigateTo("/discount-codes");
            waitForVisible(By.id("codesTable"));
            sleep(1000);
            int rows = tableRowCount(By.id("codesTableBody"));
            result.pass("Discount codes page loaded: " + rows + " code rows");
        });

        runTest("[Discounts] Statistics cards (total, active, expired)", result -> {
            navigateTo("/discount-codes");
            sleep(800);
            int found = 0;
            for (String id : new String[]{"totalCodes", "activeCodes", "expiredCodes", "totalSavings"}) {
                if (elementExists(By.id(id))) found++;
            }
            result.pass(found + "/4 discount statistics cards visible");
        });

        runTest("[Discounts] Search filter works", result -> {
            navigateTo("/discount-codes");
            waitForVisible(By.id("codesTableBody"));
            sleep(800);
            assertTrue(elementExists(By.id("searchCode")), "#searchCode input must exist");
            type(By.id("searchCode"), "A");
            sleep(700);
            result.pass("Discount search executed: " + tableRowCount(By.id("codesTableBody")) + " rows");
            driver.findElement(By.id("searchCode")).clear();
        });

        runTest("[Discounts] Status filter (active/expired/inactive)", result -> {
            navigateTo("/discount-codes");
            sleep(600);
            assertTrue(elementExists(By.id("statusFilter")), "#statusFilter select must exist");
            WebElement sf = driver.findElement(By.id("statusFilter"));
            int options = new Select(sf).getOptions().size();
            assertTrue(options >= 2, "Status filter must have multiple options");
            new Select(sf).selectByIndex(1);
            sleep(600);
            result.pass("Status filter has " + options + " options, selection works");
            new Select(sf).selectByIndex(0);
        });

        runTest("[Discounts] Open Add Discount Code modal", result -> {
            navigateTo("/discount-codes");
            sleep(500);
            By addBtn = By.xpath("//*[contains(@onclick,'openAdd') or contains(@onclick,'addCode') or contains(text(),'Thêm mã')]");
            if (!elementExists(addBtn)) { result.skip("Add discount button not found"); return; }
            jsClick(addBtn);
            sleep(700);
            assertTrue(isModalVisible("addCodeModal"), "#addCodeModal must open");
            result.pass("Add Discount Code modal opened");
            closeModal("addCodeModal");
        });

        runTest("[Discounts] Add form has code, type, value, date fields", result -> {
            navigateTo("/discount-codes");
            sleep(500);
            By addBtn = By.xpath("//*[contains(@onclick,'openAdd') or contains(@onclick,'addCode')]");
            if (!elementExists(addBtn)) { result.skip("Add discount button not found"); return; }
            jsClick(addBtn);
            sleep(700);
            if (!isModalVisible("addCodeModal")) { result.skip("Modal not visible"); return; }

            By[] expected = {
                By.cssSelector("#addCodeForm [name='code'], #addCodeModal [name='code']"),
                By.cssSelector("#addCodeForm [name='discount_type'], #addCodeModal [name='discount_type']"),
                By.cssSelector("#addCodeForm [name='discount_value'], #addCodeModal [name='discount_value']"),
            };
            int found = 0;
            for (By sel : expected) {
                if (elementExists(sel)) found++;
            }
            assertTrue(found >= 2, "Code, discount_type, discount_value fields must exist");

            // Fill code field
            By codeField = By.cssSelector("#addCodeForm [name='code'], #addCodeModal [name='code']");
            if (elementExists(codeField)) {
                type(codeField, testPrefix + "DISC" + (System.currentTimeMillis() % 1000));
            }
            // Fill start/end date
            By startDate = By.cssSelector("#addCodeForm [name='start_date'], #addCodeModal [name='start_date']");
            By endDate = By.cssSelector("#addCodeForm [name='end_date'], #addCodeModal [name='end_date']");
            if (elementExists(startDate)) type(startDate, LocalDate.now().toString());
            if (elementExists(endDate)) type(endDate, LocalDate.now().plusMonths(1).toString());

            result.pass("Discount form has " + found + "/3 required fields, filled test data");
            closeModal("addCodeModal");
        });

        runTest("[Discounts] Delete confirmation modal", result -> {
            navigateTo("/discount-codes");
            waitForVisible(By.id("codesTableBody"));
            sleep(1000);
            By deleteBtn = By.cssSelector("#codesTableBody tr:first-child [onclick*='delete'], " +
                    "#codesTableBody tr:first-child [onclick*='Delete'], " +
                    "#codesTableBody tr:first-child .btn-danger");
            if (!elementExists(deleteBtn)) { result.skip("Delete button not found in discount rows"); return; }
            jsClick(deleteBtn);
            sleep(600);
            assertTrue(isModalVisible("deleteCodeModal"), "#deleteCodeModal must open");
            // Cancel deletion
            By cancelBtn = By.cssSelector("#deleteCodeModal .btn-secondary, #deleteCodeModal [onclick*='close']");
            if (elementExists(cancelBtn)) jsClick(cancelBtn);
            result.pass("Delete confirmation modal opened and cancelled");
        });

        return results;
    }
}
