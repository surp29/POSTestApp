package com.pos.tester.tests;

import com.google.gson.JsonArray;
import com.pos.tester.api.ApiClient;
import com.pos.tester.api.TestContext;
import com.pos.tester.model.TestConfig;
import com.pos.tester.model.TestResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DiscountTestSuite extends BaseTestSuite {
    private final TestConfig config;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public DiscountTestSuite(TestContext ctx, TestConfig config) {
        super(ctx);
        this.config = config;
    }

    @Override public String getModuleName() { return "Discount Codes"; }
    @Override public String getDescription() { return "Discount CRUD, Validation, Expiry, Usage (ID-based endpoints)"; }

    @Override
    public List<TestResult> runAll() throws InterruptedException {
        results.clear();
        String testCode = config.getTestDataPrefix() + "DISC" + (System.currentTimeMillis() % 10000);
        // GET/PUT/DELETE đều dùng integer ID (không phải code string)
        // Lưu ID vào context với key "discountId"

        // 1. List discount codes
        runTest("GET /api/discount-codes/ - Danh sách mã giảm giá", result -> {
            ApiClient.ApiResponse resp = api.get("/api/discount-codes/");
            assertTrue(resp.success, "Lỗi lấy mã giảm giá: " + resp.statusCode);
            result.pass("Danh sách mã giảm giá (status " + resp.statusCode + ")");
        });

        // 2. Create percentage discount
        // Required: code, name, description, discount_type, discount_value, start_date, end_date, max_uses, min_order_value, status
        runTest("POST /api/discount-codes/ - Tạo mã giảm giá % (percentage)", result -> {
            String startDt = LocalDateTime.now().format(DT);
            String endDt   = LocalDateTime.now().plusMonths(1).format(DT);
            String body = String.format("""
                {
                    "code": "%s",
                    "name": "Mã Test %s",
                    "description": "Tạo bởi POS Tester",
                    "discount_type": "percentage",
                    "discount_value": 15,
                    "start_date": "%s",
                    "end_date": "%s",
                    "max_uses": 10,
                    "min_order_value": 100000,
                    "status": "active"
                }
                """, testCode, testCode, startDt, endDt);
            ApiClient.ApiResponse resp = api.post("/api/discount-codes/", body);
            assertTrue(resp.success, "Tạo mã giảm giá thất bại: " + resp.body);
            int discId = resp.getIntField("id");
            assertTrue(discId > 0, "id không hợp lệ: " + discId);
            ctx.set("discountId", discId);
            ctx.setCreatedDiscountCode(testCode);
            result.pass("Mã " + testCode + " tạo thành công, id=" + discId);
        });

        // 3. Get by integer ID (not code string)
        runTest("GET /api/discount-codes/{id} - Lấy mã giảm giá theo ID", result -> {
            Integer discId = ctx.getInteger("discountId");
            if (discId == null) { result.skip("Chưa tạo mã giảm giá"); return; }
            ApiClient.ApiResponse resp = api.get("/api/discount-codes/" + discId);
            assertTrue(resp.success, "Lấy mã thất bại: " + resp.body);
            String code = resp.getField("code");
            String type = resp.getField("discount_type");
            result.pass("Mã: code=" + code + ", type=" + type + ", value=" + resp.getField("discount_value"));
        });

        // 4. Create fixed discount
        runTest("POST /api/discount-codes/ - Tạo mã giảm giá cố định (fixed)", result -> {
            String fixCode = testCode + "_FIX";
            String startDt = LocalDateTime.now().format(DT);
            String endDt   = LocalDateTime.now().plusMonths(1).format(DT);
            String body = String.format("""
                {
                    "code": "%s",
                    "name": "Giảm cố định %s",
                    "description": "Test fixed discount",
                    "discount_type": "fixed",
                    "discount_value": 50000,
                    "start_date": "%s",
                    "end_date": "%s",
                    "max_uses": 5,
                    "min_order_value": 200000,
                    "status": "active"
                }
                """, fixCode, fixCode, startDt, endDt);
            ApiClient.ApiResponse resp = api.post("/api/discount-codes/", body);
            assertTrue(resp.success, "Tạo fixed discount thất bại: " + resp.body);
            result.pass("Fixed discount tạo thành công: " + fixCode);
            // Cleanup
            int fixId = resp.getIntField("id");
            if (fixId > 0) api.delete("/api/discount-codes/" + fixId);
        });

        // 5. Duplicate code rejected
        runTest("POST /api/discount-codes/ - Validation: mã trùng bị từ chối", result -> {
            String code = ctx.getCreatedDiscountCode();
            if (code == null) { result.skip("Không có mã để test trùng"); return; }
            String startDt = LocalDateTime.now().format(DT);
            String endDt   = LocalDateTime.now().plusMonths(1).format(DT);
            String body = String.format("""
                {
                    "code": "%s",
                    "name": "Duplicate Test",
                    "description": "test",
                    "discount_type": "percentage",
                    "discount_value": 5,
                    "start_date": "%s",
                    "end_date": "%s",
                    "max_uses": 1,
                    "min_order_value": 0,
                    "status": "active"
                }
                """, code, startDt, endDt);
            ApiClient.ApiResponse resp = api.post("/api/discount-codes/", body);
            assertTrue(!resp.success, "Phải từ chối mã trùng nhưng thành công: " + resp.statusCode);
            result.pass("Mã trùng bị từ chối đúng (status " + resp.statusCode + ")");
        });

        // 6. Invalid percentage > 100
        runTest("POST /api/discount-codes/ - Validation: phần trăm > 100 bị từ chối", result -> {
            String startDt = LocalDateTime.now().format(DT);
            String endDt   = LocalDateTime.now().plusMonths(1).format(DT);
            String body = String.format("""
                {
                    "code": "%s_OVER",
                    "name": "Over 100 percent",
                    "description": "test",
                    "discount_type": "percentage",
                    "discount_value": 150,
                    "start_date": "%s",
                    "end_date": "%s",
                    "max_uses": 1,
                    "min_order_value": 0,
                    "status": "active"
                }
                """, testCode, startDt, endDt);
            ApiClient.ApiResponse resp = api.post("/api/discount-codes/", body);
            assertTrue(!resp.success, "Phải từ chối > 100% nhưng thành công: " + resp.statusCode);
            result.pass("Phần trăm > 100 bị từ chối đúng (status " + resp.statusCode + ")");
        });

        // 7. Update by integer ID
        runTest("PUT /api/discount-codes/{id} - Cập nhật mã giảm giá", result -> {
            Integer discId = ctx.getInteger("discountId");
            if (discId == null) { result.skip("Chưa tạo mã giảm giá"); return; }
            String startDt = LocalDateTime.now().format(DT);
            String endDt   = LocalDateTime.now().plusMonths(2).format(DT);
            String body = String.format("""
                {"discount_value": 20, "max_uses": 50, "start_date": "%s", "end_date": "%s"}
                """, startDt, endDt);
            ApiClient.ApiResponse resp = api.put("/api/discount-codes/" + discId, body);
            assertTrue(resp.success, "Cập nhật thất bại: " + resp.body);
            result.pass("Mã giảm giá cập nhật thành công, id=" + discId);
        });

        // 8. Use discount code (apply to order)
        runTest("POST /api/discount-codes/{id}/use - Áp dụng mã giảm giá", result -> {
            Integer discId = ctx.getInteger("discountId");
            if (discId == null) { result.skip("Chưa tạo mã giảm giá"); return; }
            ApiClient.ApiResponse resp = api.post(
                "/api/discount-codes/" + discId + "/use?order_value=500000", "");
            if (resp.statusCode == 400) {
                result.skip("Mã không áp dụng được: " + resp.getField("detail"));
            } else {
                assertTrue(resp.success, "Áp dụng mã thất bại: " + resp.body);
                result.pass("Áp dụng thành công: discount_amount=" + resp.getField("discount_amount"));
            }
        });

        // 9. Delete by integer ID — response: {"message": "..."}
        runTest("DELETE /api/discount-codes/{id} - Xóa mã giảm giá", result -> {
            Integer discId = ctx.getInteger("discountId");
            if (discId == null) { result.skip("Chưa tạo mã để xóa"); return; }
            ApiClient.ApiResponse resp = api.delete("/api/discount-codes/" + discId);
            // Delete trả về {"message": "..."} chứ không phải {"success": true}
            assertTrue(resp.statusCode == 200, "Xóa thất bại: " + resp.body);
            ctx.set("discountId", null);
            ctx.setCreatedDiscountCode(null);
            result.pass("Mã giảm giá xóa thành công (status " + resp.statusCode + ")");
        });

        return results;
    }
}
