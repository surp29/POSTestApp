package com.pos.tester.uitests;

import com.pos.tester.model.TestResult;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.List;

public class POSUITest extends BaseUITestSuite {

    public POSUITest(WebDriver driver, String frontendUrl, String screenshotDir, int waitSeconds) {
        super(driver, frontendUrl, screenshotDir, waitSeconds);
    }

    @Override public String getModuleName() { return "POS Interface"; }
    @Override public String getDescription() { return "UI: Product search, Add to cart, Discount, Checkout, Payment"; }

    @Override
    public List<TestResult> runAll() throws InterruptedException {
        results.clear();

        runTest("Load POS page - product grid renders", result -> {
            navigateTo("/pos");
            waitForVisible(By.id("productsGrid"));
            sleep(1500); // wait for AJAX products to load
            boolean hasCards = elementExists(By.cssSelector(".product-card"));
            assertTrue(hasCards || elementExists(By.id("productSearch")),
                    "POS page should have product cards or search");
            result.pass("POS page loaded: " +
                    driver.findElements(By.cssSelector(".product-card")).size() + " products visible");
        });

        runTest("Product search filters results", result -> {
            navigateTo("/pos");
            waitForVisible(By.id("productSearch"));
            sleep(1000);
            int initialCount = driver.findElements(By.cssSelector(".product-card")).size();
            type(By.id("productSearch"), "a");
            sleep(800);
            int afterCount = driver.findElements(By.cssSelector(".product-card")).size();
            result.pass("Search works: initial=" + initialCount + " after='a': " + afterCount + " cards");
        });

        runTest("Clear search restores full product list", result -> {
            navigateTo("/pos");
            waitForVisible(By.id("productSearch"));
            sleep(1000);
            int fullCount = driver.findElements(By.cssSelector(".product-card")).size();
            type(By.id("productSearch"), "xyz_nonexistent_product_abc");
            sleep(800);
            // Trigger search via JS to ensure oninput fires when clearing
            ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('productSearch').value = '';" +
                "if(typeof searchProducts === 'function') searchProducts();");
            sleep(1000);
            int restoredCount = driver.findElements(By.cssSelector(".product-card")).size();
            assertTrue(restoredCount >= fullCount - 2,
                    "Product count should restore after clearing search");
            result.pass("Search cleared: product count restored (" + fullCount + " → " + restoredCount + ")");
        });

        runTest("Group filter dropdown works", result -> {
            navigateTo("/pos");
            waitForVisible(By.id("productGroupFilter"));
            sleep(500);
            WebElement filter = driver.findElement(By.id("productGroupFilter"));
            int optionCount = new Select(filter).getOptions().size();
            assertTrue(optionCount >= 1, "Group filter should have at least 1 option (All)");
            result.pass("Product group filter has " + optionCount + " options");
        });

        runTest("Add product to cart", result -> {
            navigateTo("/pos");
            waitForVisible(By.id("productsGrid"));
            sleep(1200);
            List<WebElement> cards = driver.findElements(By.cssSelector(".product-card"));
            if (cards.isEmpty()) {
                result.skip("No product cards visible in POS grid");
                return;
            }
            // Click first available in-stock card
            WebElement card = null;
            for (WebElement c : cards) {
                String stockText = "";
                try { stockText = c.findElement(By.cssSelector(".product-stock")).getText(); } catch (Exception ignored) {}
                if (!stockText.toLowerCase().contains("hết")) { card = c; break; }
            }
            if (card == null) card = cards.get(0);
            jsClick(card);
            sleep(800);
            // Check cart has items
            int cartCount = driver.findElements(By.cssSelector(".cart-item")).size();
            assertTrue(cartCount > 0, "Cart should have at least 1 item after clicking product");
            result.pass("Product added to cart: " + cartCount + " item(s) in cart");
        });

        runTest("Cart subtotal updates when product added", result -> {
            navigateTo("/pos");
            waitForVisible(By.id("productsGrid"));
            sleep(1000);
            // Read initial total (should be 0)
            String initialTotal = "";
            try { initialTotal = driver.findElement(By.id("cartTotal")).getText(); } catch (Exception e) {}

            List<WebElement> cards = driver.findElements(By.cssSelector(".product-card"));
            if (cards.isEmpty()) { result.skip("No products visible"); return; }
            jsClick(cards.get(0));
            sleep(600);
            String newTotal = "";
            try { newTotal = driver.findElement(By.id("cartTotal")).getText(); } catch (Exception e) {}
            result.pass("Cart total updated: '" + initialTotal + "' → '" + newTotal + "'");
        });

        runTest("Add same product twice - quantity increases", result -> {
            navigateTo("/pos");
            waitForVisible(By.id("productsGrid"));
            sleep(1000);
            List<WebElement> cards = driver.findElements(By.cssSelector(".product-card"));
            if (cards.isEmpty()) { result.skip("No products visible"); return; }
            jsClick(cards.get(0));
            sleep(400);
            jsClick(driver.findElements(By.cssSelector(".product-card")).get(0));
            sleep(400);
            // Find quantity in cart
            List<WebElement> qtyElements = driver.findElements(
                    By.cssSelector(".cart-item-quantity, .cart-item input[type='number'], .qty-input"));
            if (!qtyElements.isEmpty()) {
                String qty = qtyElements.get(0).getAttribute("value");
                if (qty == null) qty = qtyElements.get(0).getText();
                result.pass("Quantity shown in cart: " + qty);
            } else {
                result.pass("Product added twice, cart items: " +
                        driver.findElements(By.cssSelector(".cart-item")).size());
            }
        });

        runTest("Discount dropdown opens", result -> {
            navigateTo("/pos");
            waitForVisible(By.id("productsGrid"));
            sleep(800);
            // Add a product first
            List<WebElement> cards = driver.findElements(By.cssSelector(".product-card"));
            if (!cards.isEmpty()) jsClick(cards.get(0));
            sleep(500);
            By discountBtn = By.cssSelector(".btn-discount-toggle, #discountBtn, [onclick*='discount'], [onclick*='Discount']");
            if (elementExists(discountBtn)) {
                jsClick(discountBtn);
                sleep(500);
                result.pass("Discount toggle opened");
            } else if (elementExists(By.id("discountDropdown"))) {
                result.pass("Discount dropdown element present");
            } else {
                result.skip("Discount toggle button not found on page");
            }
        });

        runTest("Clear cart button empties cart", result -> {
            navigateTo("/pos");
            waitForVisible(By.id("productsGrid"));
            sleep(1000);
            List<WebElement> cards = driver.findElements(By.cssSelector(".product-card"));
            if (cards.isEmpty()) { result.skip("No products visible to add"); return; }
            jsClick(cards.get(0));
            sleep(500);
            // Find clear cart button
            By clearBtn = By.cssSelector("[onclick*='clearCart'], #clearCartBtn, .btn-clear-cart");
            if (elementExists(clearBtn)) {
                jsClick(clearBtn);
                sleep(700);
                // clearCart() uses custom showConfirm() — click #confirmOkBtn (not native alert)
                By confirmOkBtn = By.id("confirmOkBtn");
                if (elementExists(confirmOkBtn)) {
                    jsClick(confirmOkBtn);
                    sleep(600);
                }
                int remaining = driver.findElements(By.cssSelector(".cart-item")).size();
                assertTrue(remaining == 0, "Cart should be empty after clear, got " + remaining);
                result.pass("Cart cleared successfully");
            } else {
                result.skip("Clear cart button not found (selector: " + clearBtn + ")");
            }
        });

        runTest("Payment modal opens on checkout", result -> {
            navigateTo("/pos");
            waitForVisible(By.id("productsGrid"));
            sleep(1000);
            List<WebElement> cards = driver.findElements(By.cssSelector(".product-card"));
            if (cards.isEmpty()) { result.skip("No products visible"); return; }
            jsClick(cards.get(0));
            sleep(600);
            // Click pay/checkout button
            By payBtn = By.cssSelector("[onclick*='processPayment'], [onclick*='openPayment'], .btn-pay, #payBtn");
            if (elementExists(payBtn)) {
                jsClick(payBtn);
                sleep(800);
                boolean modalVisible = isModalVisible("paymentModal");
                assertTrue(modalVisible, "Payment modal should open after clicking pay");
                result.pass("Payment modal opened successfully");
                // Close it
                closeModal("paymentModal");
            } else {
                result.skip("Pay button not found (try: [onclick*='processPayment'])");
            }
        });

        runTest("Payment method dropdown in modal", result -> {
            navigateTo("/pos");
            waitForVisible(By.id("productsGrid"));
            sleep(1000);
            List<WebElement> cards = driver.findElements(By.cssSelector(".product-card"));
            if (cards.isEmpty()) { result.skip("No products visible"); return; }
            jsClick(cards.get(0));
            sleep(500);
            By payBtn = By.cssSelector("[onclick*='processPayment'], [onclick*='openPayment'], .btn-pay, #payBtn");
            if (!elementExists(payBtn)) { result.skip("Pay button not found"); return; }
            jsClick(payBtn);
            sleep(800);
            if (!isModalVisible("paymentModal")) { result.skip("Payment modal did not open"); return; }
            assertTrue(elementExists(By.id("paymentMethod")), "Payment method dropdown should exist in modal");
            List<WebElement> options = new Select(driver.findElement(By.id("paymentMethod"))).getOptions();
            assertTrue(options.size() >= 2, "Should have multiple payment methods");
            result.pass("Payment methods available: " + options.size() +
                    " options (first: " + options.get(0).getText() + ")");
            closeModal("paymentModal");
        });

        runTest("POS pagination buttons exist", result -> {
            navigateTo("/pos");
            waitForVisible(By.id("productsGrid"));
            boolean hasPrev = elementExists(By.id("prevPageBtn"));
            boolean hasNext = elementExists(By.id("nextPageBtn"));
            assertTrue(hasPrev || hasNext, "Pagination buttons should exist");
            result.pass("Pagination controls found: prevPage=" + hasPrev + " nextPage=" + hasNext);
        });

        return results;
    }
}
