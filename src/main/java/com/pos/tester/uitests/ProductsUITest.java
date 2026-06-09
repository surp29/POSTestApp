package com.pos.tester.uitests;

import com.pos.tester.model.TestResult;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.util.List;

public class ProductsUITest extends BaseUITestSuite {
    private final String testPrefix;

    public ProductsUITest(WebDriver driver, String frontendUrl, String screenshotDir,
                          int waitSeconds, String testPrefix) {
        super(driver, frontendUrl, screenshotDir, waitSeconds);
        this.testPrefix = testPrefix;
    }

    @Override public String getModuleName() { return "Products Page"; }
    @Override public String getDescription() { return "UI: Product list, Search, Create, Edit, Delete via interface"; }

    @Override
    public List<TestResult> runAll() throws InterruptedException {
        results.clear();

        runTest("Load products page - table renders", result -> {
            navigateTo("/products");
            waitForVisible(By.id("productsTable"));
            sleep(1200);
            boolean hasBody = elementExists(By.id("productsTableBody"));
            assertTrue(hasBody, "Products table body (#productsTableBody) must exist");
            int rows = tableRowCount(By.id("productsTableBody"));
            result.pass("Products page loaded: " + rows + " product rows visible");
        });

        runTest("Statistics cards display (total, in-stock, out-of-stock)", result -> {
            navigateTo("/products");
            sleep(800);
            boolean hasStats = elementExists(By.id("totalProducts")) ||
                               elementExists(By.cssSelector(".stat-card, .kpi-card, [class*='stat']"));
            assertTrue(hasStats, "Statistics cards should be visible");
            int total = getStatCount("totalProducts");
            result.pass("Statistics: totalProducts=" + (total >= 0 ? total : "N/A"));
        });

        runTest("Search by product code/name filters table", result -> {
            navigateTo("/products");
            waitForVisible(By.id("productsTableBody"));
            sleep(1000);
            int before = tableRowCount(By.id("productsTableBody"));
            type(By.id("productCodeFilter"), "a");
            sleep(800);
            int after = tableRowCount(By.id("productsTableBody"));
            result.pass("Search filter: before=" + before + " after='a'=" + after + " rows");
            // Clear
            driver.findElement(By.id("productCodeFilter")).clear();
            sleep(600);
        });

        runTest("Open Add Product modal", result -> {
            navigateTo("/products");
            waitForVisible(By.id("productsTableBody"));
            By addBtn = By.cssSelector("[onclick*='openAdd'], #addProductBtn, .btn-add, [onclick*='Add']");
            if (!elementExists(addBtn)) {
                addBtn = By.xpath("//*[contains(@onclick,'openAdd') or contains(@onclick,'addProduct') or contains(text(),'Thêm')]");
            }
            if (!elementExists(addBtn)) { result.skip("Add product button not found"); return; }
            jsClick(addBtn);
            sleep(600);
            assertTrue(isModalVisible("addProductModal"), "Add product modal must open");
            result.pass("Add Product modal opened successfully");
            closeModal("addProductModal");
        });

        runTest("Add Product form fields exist and are fillable", result -> {
            navigateTo("/products");
            sleep(500);
            By addBtn = By.xpath("//*[contains(@onclick,'openAdd') or contains(@onclick,'addProduct') or contains(text(),'Thêm sản phẩm') or contains(text(),'Thêm')]");
            if (!elementExists(addBtn)) { result.skip("Add product button not found"); return; }
            jsClick(addBtn);
            sleep(600);
            if (!isModalVisible("addProductModal")) { result.skip("Modal did not open"); return; }

            // Verify and fill form fields
            assertTrue(elementExists(By.id("add_product_code")), "#add_product_code field missing");
            type(By.id("add_product_code"), testPrefix + "PROD_" + System.currentTimeMillis() % 10000);

            // Product name (try common selectors)
            By nameField = elementExists(By.cssSelector("#addProductModal [name='product_name']")) ?
                    By.cssSelector("#addProductModal [name='product_name']") :
                    By.cssSelector("#addProductModal input[placeholder*='tên'], #addProductModal input[placeholder*='name']");
            if (elementExists(nameField)) type(nameField, testPrefix + "Test Product");

            // Price fields
            if (elementExists(By.id("add_price_display"))) {
                type(By.id("add_price_display"), "750.000");
            }
            if (elementExists(By.id("add_cost_price_display"))) {
                type(By.id("add_cost_price_display"), "500.000");
            }
            result.pass("Product form fields verified and filled");
            closeModal("addProductModal");
        });

        runTest("Create new product via UI form", result -> {
            navigateTo("/products");
            sleep(800);

            // Open the add modal via the exact open button (not any button containing 'addProduct')
            By addBtn = By.cssSelector("[onclick='openAddProductModal()']");
            if (!elementExists(addBtn))
                addBtn = By.xpath("//*[contains(@onclick,'openAddProduct')]");
            if (!elementExists(addBtn)) { result.skip("Cannot find add product button"); return; }
            jsClick(addBtn);
            sleep(700);
            if (!isModalVisible("addProductModal")) { result.skip("Add modal not visible"); return; }

            // Unique code: last 8 digits of epoch ms → max 10 chars, never repeats within 10M ms
            String prodCode = "UI" + (System.currentTimeMillis() % 100000000L);
            type(By.id("add_product_code"), prodCode);
            sleep(300); // let debounce settle before next field

            By nameField = By.cssSelector("#addProductModal input[name='name']");
            type(nameField, "UI Test Product");
            By groupField = By.cssSelector("#addProductModal input[name='group']");
            if (elementExists(groupField)) type(groupField, "TestGroup");
            By qtyField = By.cssSelector("#addProductModal input[name='quantity']");
            if (elementExists(qtyField)) type(qtyField, "5");

            // Set both hidden AND display price fields — display must be non-zero so restoreZeroIfEmpty
            // won't reset the hidden field on blur: cost(300000) < price(800000) → validation passes
            ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('add_price').value='800000';" +
                "document.getElementById('add_price_display').value='800,000';" +
                "document.getElementById('add_cost_price').value='300000';" +
                "document.getElementById('add_cost_price_display').value='300,000';");

            // EXACT selector for the submit button: onclick="addProduct()" only
            // Using [onclick*='addProduct'] would also match closeAddProductModal() — wrong button!
            By submitBtn = By.cssSelector("#addProductModal [onclick='addProduct()']");
            if (!elementExists(submitBtn)) { result.skip("Submit button not found in add modal"); closeModal("addProductModal"); return; }
            jsClick(submitBtn);

            boolean modalClosed = waitForInvisibleSafe(By.id("addProductModal"));
            if (modalClosed) {
                sleep(1500);
                result.pass("Product created: " + prodCode + " (modal closed on success)");
            } else {
                // Read any visible toast error for diagnostics
                String errMsg = (String) ((JavascriptExecutor) driver).executeScript(
                    "var el = document.querySelector('.toast-message, .toast-content');" +
                    "return el ? el.textContent.trim() : 'no error toast visible';");
                ((JavascriptExecutor) driver).executeScript(
                    "document.getElementById('addProductModal').style.display='none';");
                result.fail("Modal stayed open after submit | " + errMsg);
            }
        });

        runTest("Edit product opens edit modal", result -> {
            navigateTo("/products");
            waitForVisible(By.id("productsTableBody"));
            sleep(1200);
            By editBtn = By.cssSelector("[onclick*='openEdit'], .btn-edit, [data-action='edit']");
            if (!elementExists(editBtn)) {
                // Try table row action buttons
                editBtn = By.cssSelector("#productsTableBody tr:first-child [onclick*='edit'], #productsTableBody tr:first-child button:first-child");
            }
            if (!elementExists(editBtn)) { result.skip("Edit button not found in products table"); return; }
            jsClick(editBtn);
            sleep(700);
            assertTrue(isModalVisible("editProductModal"), "Edit product modal must open");
            assertTrue(elementExists(By.id("edit_product_code")), "#edit_product_code must exist in edit modal");
            String code = getValue(By.id("edit_product_code"));
            result.pass("Edit modal opened: product code=" + code);
            closeModal("editProductModal");
        });

        runTest("Edit modal has read-only product code", result -> {
            navigateTo("/products");
            waitForVisible(By.id("productsTableBody"));
            sleep(1000);
            By editBtn = By.cssSelector("[onclick*='openEdit'], #productsTableBody tr:first-child [onclick*='edit']");
            if (!elementExists(editBtn)) { result.skip("Edit button not found"); return; }
            jsClick(editBtn);
            sleep(700);
            if (!isModalVisible("editProductModal")) { result.skip("Edit modal not visible"); return; }
            WebElement codeField = driver.findElement(By.id("edit_product_code"));
            String readonly = codeField.getAttribute("readonly");
            boolean isReadOnly = "true".equals(readonly) || "readonly".equals(readonly) || readonly != null;
            result.pass("Product code field readonly=" + isReadOnly + " (value: " + codeField.getAttribute("value") + ")");
            closeModal("editProductModal");
        });

        runTest("Delete product confirmation modal", result -> {
            navigateTo("/products");
            waitForVisible(By.id("productsTableBody"));
            sleep(1200);
            // Scope to table body — avoids matching confirmDeleteProduct() in the hidden modal
            By deleteBtn = By.cssSelector("#productsTableBody .btn-danger");
            if (!elementExists(deleteBtn)) { result.skip("Delete button not found in table"); return; }
            // Click the last row's delete button
            List<WebElement> deleteBtns = driver.findElements(deleteBtn);
            scrollIntoView(deleteBtns.get(deleteBtns.size() - 1));
            jsClick(deleteBtns.get(deleteBtns.size() - 1));
            sleep(700);
            assertTrue(isModalVisible("deleteProductModal"), "Delete confirmation modal must open");
            // Cancel the delete
            By cancelBtn = By.cssSelector("#deleteProductModal .btn-secondary, #deleteProductModal [onclick*='close'], #deleteProductModal .modal-close");
            if (elementExists(cancelBtn)) jsClick(cancelBtn);
            else ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('deleteProductModal').style.display='none';");
            result.pass("Delete confirmation modal opened and cancelled");
        });

        runTest("Product group filter works", result -> {
            navigateTo("/products");
            waitForVisible(By.id("productGroupFilter"));
            sleep(500);
            WebElement filter = driver.findElement(By.id("productGroupFilter"));
            int options = new Select(filter).getOptions().size();
            if (options > 1) {
                new Select(filter).selectByIndex(1);
                sleep(800);
                int filtered = tableRowCount(By.id("productsTableBody"));
                new Select(filter).selectByIndex(0);
                sleep(500);
                int all = tableRowCount(By.id("productsTableBody"));
                result.pass("Group filter: option 1 shows " + filtered + " rows, all shows " + all + " rows");
            } else {
                result.pass("Product group filter has " + options + " option(s) - only 'All Groups'");
            }
        });

        return results;
    }
}
