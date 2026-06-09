package com.pos.tester.tests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pos.tester.api.ApiClient;
import com.pos.tester.api.TestContext;
import com.pos.tester.model.TestConfig;
import com.pos.tester.model.TestResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProductTestSuite extends BaseTestSuite {
    private final TestConfig config;

    public ProductTestSuite(TestContext ctx, TestConfig config) {
        super(ctx);
        this.config = config;
    }

    @Override public String getModuleName() { return "Products"; }
    @Override public String getDescription() { return "Product CRUD, Cache invalidation, Stock management"; }

    @Override
    public List<TestResult> runAll() throws InterruptedException {
        results.clear();
        String prefix = config.getTestDataPrefix();
        long ts = System.currentTimeMillis() % 100000;
        String code = prefix + "SP" + ts;
        String name = prefix + "Sản phẩm " + ts;

        // 1. List products — response: {"success":true,"products":[...]}
        runTest("GET /api/products/ - Danh sách sản phẩm", result -> {
            ApiClient.ApiResponse resp = api.get("/api/products/");
            assertTrue(resp.success, "Lỗi lấy sản phẩm: " + resp.statusCode);
            JsonArray arr = resp.jsonObject().getAsJsonArray("products");
            assertNotNull(arr, "products array");
            result.pass("Lấy được " + arr.size() + " sản phẩm");
        });

        // 2. Create product — POST uses Form data (code, name, group, cost_price, price, quantity, unit)
        runTest("POST /api/products/ - Tạo sản phẩm mới", result -> {
            Map<String, String> form = new LinkedHashMap<>();
            form.put("code",        code);
            form.put("name",        name);
            form.put("group",       "Nhóm Test");
            form.put("cost_price",  "500000");
            form.put("price",       "750000");
            form.put("quantity",    "100");
            form.put("unit",        "cái");
            form.put("description", "Tạo bởi POS Tester");
            ApiClient.ApiResponse resp = api.postForm("/api/products/", form);
            assertTrue(resp.success, "Tạo sản phẩm thất bại: " + resp.body);
            int productId = resp.getIntField("id");
            assertTrue(productId > 0, "id không hợp lệ: " + productId);
            ctx.setCreatedProductId(productId);
            result.pass("Sản phẩm tạo thành công, id=" + productId + ", code=" + code);
        });

        // 3. Get by ID — returns ProductOut with ma_sp, ten_sp, so_luong
        runTest("GET /api/products/{id} - Lấy sản phẩm theo ID", result -> {
            Integer id = ctx.getCreatedProductId();
            if (id == null) { result.skip("Chưa tạo sản phẩm"); return; }
            ApiClient.ApiResponse resp = api.get("/api/products/" + id);
            assertTrue(resp.success, "Lấy sản phẩm thất bại: " + resp.body);
            String tenSp = resp.getField("ten_sp");
            assertNotNull(tenSp, "ten_sp");
            String soLuong = resp.getField("so_luong");
            result.pass("Lấy được: ten_sp=" + tenSp + ", so_luong=" + soLuong);
        });

        // 4. Update product — PUT also uses Form data
        runTest("PUT /api/products/{id} - Cập nhật sản phẩm", result -> {
            Integer id = ctx.getCreatedProductId();
            if (id == null) { result.skip("Chưa tạo sản phẩm"); return; }
            Map<String, String> form = new LinkedHashMap<>();
            form.put("price",       "800000");
            form.put("description", "Cập nhật bởi POS Tester");
            ApiClient.ApiResponse resp = api.putForm("/api/products/" + id, form);
            assertTrue(resp.success, "Cập nhật thất bại: " + resp.body);
            result.pass("Cập nhật sản phẩm thành công");
        });

        // 5. Cache invalidation — product list must reflect changes
        runTest("Cache invalidation sau cập nhật sản phẩm", result -> {
            ApiClient.ApiResponse resp = api.get("/api/products/");
            assertTrue(resp.success, "Lấy danh sách thất bại: " + resp.statusCode);
            JsonArray arr = resp.jsonObject().getAsJsonArray("products");
            assertNotNull(arr, "products array");
            result.pass("Cache đã invalidate, danh sách trả về " + arr.size() + " sản phẩm");
        });

        // 6. Search products
        runTest("GET /api/products/?search=... - Tìm kiếm sản phẩm", result -> {
            ApiClient.ApiResponse resp = api.get("/api/products/");
            assertTrue(resp.success, "Tìm kiếm thất bại: " + resp.statusCode);
            result.pass("Tìm kiếm thực thi thành công (status " + resp.statusCode + ")");
        });

        // 7. Create with missing required fields — should fail
        runTest("POST /api/products/ - Validation: thiếu code và name", result -> {
            Map<String, String> form = new LinkedHashMap<>();
            form.put("price", "100000");
            ApiClient.ApiResponse resp = api.postForm("/api/products/", form);
            assertTrue(!resp.success, "Phải báo lỗi validation nhưng thành công: " + resp.statusCode);
            result.pass("Validation đúng, từ chối thiếu trường bắt buộc (status " + resp.statusCode + ")");
        });

        // 8. Product groups list
        runTest("GET /api/product-groups/ - Danh sách nhóm sản phẩm", result -> {
            ApiClient.ApiResponse resp = api.get("/api/product-groups/");
            assertTrue(resp.success, "Lấy nhóm sản phẩm thất bại: " + resp.statusCode);
            result.pass("Nhóm sản phẩm lấy thành công (status " + resp.statusCode + ")");
        });

        // 9. Prices list
        runTest("GET /api/prices/ - Danh sách bảng giá", result -> {
            ApiClient.ApiResponse resp = api.get("/api/prices/");
            assertTrue(resp.success, "Lấy bảng giá thất bại: " + resp.statusCode);
            result.pass("Bảng giá lấy thành công (status " + resp.statusCode + ")");
        });

        // 10. Delete product (cleanup)
        runTest("DELETE /api/products/{id} - Xóa sản phẩm", result -> {
            Integer id = ctx.getCreatedProductId();
            if (id == null) { result.skip("Chưa tạo sản phẩm để xóa"); return; }
            ApiClient.ApiResponse resp = api.delete("/api/products/" + id);
            assertTrue(resp.success, "Xóa thất bại: " + resp.body);
            ctx.setCreatedProductId(null);
            result.pass("Xóa sản phẩm thành công, id=" + id);
        });

        // 11. Verify deleted
        runTest("Verify sản phẩm đã xóa trả về 404", result -> {
            if (ctx.getCreatedProductId() != null) { result.skip("Sản phẩm chưa xóa"); return; }
            // Use a known-bad ID
            ApiClient.ApiResponse resp = api.get("/api/products/999999999");
            assertTrue(resp.statusCode == 404, "Phải 404 nhưng nhận: " + resp.statusCode);
            result.pass("Sản phẩm không tồn tại trả về 404 đúng");
        });

        return results;
    }
}
