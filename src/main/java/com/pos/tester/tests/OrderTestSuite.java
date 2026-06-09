package com.pos.tester.tests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pos.tester.api.ApiClient;
import com.pos.tester.api.TestContext;
import com.pos.tester.model.TestConfig;
import com.pos.tester.model.TestResult;

import java.time.LocalDate;
import java.util.List;

public class OrderTestSuite extends BaseTestSuite {
    private final TestConfig config;

    public OrderTestSuite(TestContext ctx, TestConfig config) {
        super(ctx);
        this.config = config;
    }

    @Override public String getModuleName() { return "Orders"; }
    @Override public String getDescription() { return "Order CRUD, Status flow (cho_xu_ly→hoan_thanh), Validation"; }

    @Override
    public List<TestResult> runAll() throws InterruptedException {
        results.clear();
        String prefix  = config.getTestDataPrefix();
        long   ts      = System.currentTimeMillis() % 100000;
        // Fields: ma_don_hang, thong_tin_kh, sp_banggia, ngay_tao, so_luong, tong_tien, trang_thai
        String maDon   = prefix + "DH" + ts;
        String today   = LocalDate.now().toString();

        // Setup: lấy mã sản phẩm đầu tiên để dùng làm sp_banggia
        final String[] spCode = {null};

        runTest("Setup: Lấy mã sản phẩm để tạo đơn hàng", result -> {
            ApiClient.ApiResponse resp = api.get("/api/products/");
            assertTrue(resp.success, "Không thể lấy sản phẩm: " + resp.statusCode);
            JsonArray products = resp.jsonObject().getAsJsonArray("products");
            if (products != null && products.size() > 0) {
                JsonObject p = products.get(0).getAsJsonObject();
                spCode[0] = p.has("ma_sp") ? p.get("ma_sp").getAsString() : null;
            }
            if (spCode[0] == null) {
                result.skip("Không có sản phẩm nào trong hệ thống");
            } else {
                result.pass("Sẽ dùng sp_banggia=" + spCode[0]);
            }
        });

        // 1. List orders — response: {"success":true, "total":n, "orders":[...]}
        runTest("GET /api/orders/ - Danh sách đơn hàng", result -> {
            ApiClient.ApiResponse resp = api.get("/api/orders/");
            assertTrue(resp.success, "Lỗi lấy đơn hàng: " + resp.statusCode);
            JsonArray orders = resp.jsonObject().getAsJsonArray("orders");
            int total = resp.getIntField("total");
            result.pass("Lấy được " + (orders != null ? orders.size() : 0) + " đơn hàng, total=" + total);
        });

        // 2. Check duplicate before create
        runTest("GET /api/orders/check-duplicate - Kiểm tra mã đơn trùng", result -> {
            ApiClient.ApiResponse resp = api.get("/api/orders/check-duplicate?ma_don_hang=" + maDon);
            assertTrue(resp.success, "Kiểm tra trùng thất bại: " + resp.statusCode);
            boolean exists = "true".equals(resp.getField("exists"));
            result.pass("Mã đơn " + maDon + (exists ? " đã tồn tại" : " chưa tồn tại") + " → hợp lệ");
        });

        // 3. Create order — fields: ma_don_hang, thong_tin_kh, sp_banggia, ngay_tao, so_luong, trang_thai
        runTest("POST /api/orders/ - Tạo đơn hàng mới", result -> {
            String spBg = spCode[0] != null ? "\"" + spCode[0] + "\"" : "null";
            String body = String.format("""
                {
                    "ma_don_hang": "%s",
                    "thong_tin_kh": "Khách Test %d",
                    "sp_banggia": %s,
                    "ngay_tao": "%s",
                    "so_luong": 2,
                    "tong_tien": 1500000,
                    "trang_thai": "cho_xu_ly"
                }
                """, maDon, ts, spBg, today);
            ApiClient.ApiResponse resp = api.post("/api/orders/", body);
            assertTrue(resp.success, "Tạo đơn hàng thất bại: " + resp.body);
            int orderId = resp.getIntField("id");
            assertTrue(orderId > 0, "id không hợp lệ: " + orderId);
            ctx.setCreatedOrderId(orderId);
            result.pass("Đơn hàng tạo thành công, id=" + orderId + ", mã=" + maDon);
        });

        // 4. Get by ID — returns OrderOut: ma_don_hang, thong_tin_kh, trang_thai, tong_tien
        runTest("GET /api/orders/{id} - Lấy đơn hàng theo ID", result -> {
            Integer orderId = ctx.getCreatedOrderId();
            if (orderId == null) { result.skip("Chưa tạo đơn hàng"); return; }
            ApiClient.ApiResponse resp = api.get("/api/orders/" + orderId);
            assertTrue(resp.success, "Lấy đơn hàng thất bại: " + resp.body);
            String maDonRes    = resp.getField("ma_don_hang");
            String trangThai   = resp.getField("trang_thai");
            assertNotNull(maDonRes, "ma_don_hang");
            result.pass("Đơn hàng: ma=" + maDonRes + ", trang_thai=" + trangThai);
        });

        // 5. Update status — fields: trang_thai, thong_tin_kh, tong_tien
        runTest("PUT /api/orders/{id} - Cập nhật trạng thái đơn hàng", result -> {
            Integer orderId = ctx.getCreatedOrderId();
            if (orderId == null) { result.skip("Chưa tạo đơn hàng"); return; }
            String body = """
                {"trang_thai": "dang_xu_ly", "thong_tin_kh": "Khách Test đã cập nhật"}
                """;
            ApiClient.ApiResponse resp = api.put("/api/orders/" + orderId, body);
            assertTrue(resp.success, "Cập nhật thất bại: " + resp.body);
            result.pass("Cập nhật đơn hàng thành công");
        });

        // 6. Duplicate code rejected
        runTest("POST /api/orders/ - Validation: mã đơn trùng bị từ chối", result -> {
            Integer orderId = ctx.getCreatedOrderId();
            if (orderId == null) { result.skip("Chưa tạo đơn hàng"); return; }
            String body = String.format("""
                {
                    "ma_don_hang": "%s",
                    "thong_tin_kh": "Duplicate Test",
                    "so_luong": 1,
                    "trang_thai": "cho_xu_ly"
                }
                """, maDon);
            ApiClient.ApiResponse resp = api.post("/api/orders/", body);
            assertTrue(!resp.success, "Phải từ chối mã trùng nhưng thành công: " + resp.statusCode);
            result.pass("Mã đơn trùng bị từ chối đúng (status " + resp.statusCode + ")");
        });

        // 7. Missing required fields
        runTest("POST /api/orders/ - Validation: thiếu ma_don_hang bị từ chối", result -> {
            String body = """
                {"thong_tin_kh": "Khách Test", "so_luong": 1}
                """;
            ApiClient.ApiResponse resp = api.post("/api/orders/", body);
            assertTrue(!resp.success, "Phải từ chối thiếu mã đơn nhưng thành công: " + resp.statusCode);
            result.pass("Thiếu ma_don_hang bị từ chối đúng (status " + resp.statusCode + ")");
        });

        // 8. Delete order (cleanup)
        runTest("DELETE /api/orders/{id} - Xóa đơn hàng", result -> {
            Integer orderId = ctx.getCreatedOrderId();
            if (orderId == null) { result.skip("Chưa tạo đơn để xóa"); return; }
            ApiClient.ApiResponse resp = api.delete("/api/orders/" + orderId);
            assertTrue(resp.success, "Xóa đơn hàng thất bại: " + resp.body);
            ctx.setCreatedOrderId(null);
            result.pass("Xóa đơn hàng thành công");
        });

        return results;
    }
}
