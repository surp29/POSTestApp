package com.pos.tester.tests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pos.tester.api.ApiClient;
import com.pos.tester.api.TestContext;
import com.pos.tester.model.TestConfig;
import com.pos.tester.model.TestResult;

import java.util.List;

public class CustomerWarehouseTestSuite extends BaseTestSuite {
    private final TestConfig config;

    public CustomerWarehouseTestSuite(TestContext ctx, TestConfig config) {
        super(ctx);
        this.config = config;
    }

    @Override public String getModuleName() { return "Customers & Warehouse"; }
    @Override public String getDescription() { return "Customer CRM (/api/accounts/), Warehouse (/api/warehouse/), Areas, Shops"; }

    @Override
    public List<TestResult> runAll() throws InterruptedException {
        results.clear();
        String prefix = config.getTestDataPrefix();
        long   ts     = System.currentTimeMillis() % 100000;

        // ═══ CUSTOMER (ACCOUNT) TESTS ════════════════════════════════════════════
        // AccountCreate: ten_tk(str), ma_khach_hang(str), ngay_sinh(date), email, so_dt, dia_chi, trang_thai(bool)

        runTest("GET /api/accounts/ - Danh sách khách hàng", result -> {
            ApiClient.ApiResponse resp = api.get("/api/accounts/");
            assertTrue(resp.success, "Lỗi lấy khách hàng: " + resp.statusCode);
            result.pass("Danh sách khách hàng (status " + resp.statusCode + ")");
        });

        runTest("POST /api/accounts/ - Tạo khách hàng mới", result -> {
            String body = String.format("""
                {
                    "ten_tk": "%sKhách %d",
                    "ma_khach_hang": "%sKH%d",
                    "email": "test%d@postest.com",
                    "so_dt": "09%d",
                    "dia_chi": "123 Đường Test",
                    "trang_thai": true
                }
                """, prefix, ts, prefix, ts, ts, ts % 100000000L);
            ApiClient.ApiResponse resp = api.post("/api/accounts/", body);
            assertTrue(resp.success, "Tạo khách hàng thất bại: " + resp.body);
            int accId = resp.getIntField("id");
            assertTrue(accId > 0, "id không hợp lệ: " + accId);
            ctx.setCreatedAccountId(accId);
            result.pass("Khách hàng tạo thành công, id=" + accId);
        });

        runTest("GET /api/accounts/{id} - Lấy khách hàng theo ID", result -> {
            Integer accId = ctx.getCreatedAccountId();
            if (accId == null) { result.skip("Chưa tạo khách hàng"); return; }
            ApiClient.ApiResponse resp = api.get("/api/accounts/" + accId);
            assertTrue(resp.success, "Lấy khách hàng thất bại: " + resp.body);
            String tenTk = resp.getField("ten_tk");
            assertNotNull(tenTk, "ten_tk");
            result.pass("Khách hàng: ten_tk=" + tenTk);
        });

        runTest("PUT /api/accounts/{id} - Cập nhật khách hàng", result -> {
            Integer accId = ctx.getCreatedAccountId();
            if (accId == null) { result.skip("Chưa tạo khách hàng"); return; }
            String body = """
                {"dia_chi": "456 Đường Cập Nhật", "trang_thai": true}
                """;
            ApiClient.ApiResponse resp = api.put("/api/accounts/" + accId, body);
            assertTrue(resp.success, "Cập nhật khách hàng thất bại: " + resp.body);
            result.pass("Khách hàng cập nhật thành công");
        });

        runTest("DELETE /api/accounts/{id} - Xóa khách hàng", result -> {
            Integer accId = ctx.getCreatedAccountId();
            if (accId == null) { result.skip("Chưa tạo khách hàng để xóa"); return; }
            ApiClient.ApiResponse resp = api.delete("/api/accounts/" + accId);
            assertTrue(resp.success, "Xóa khách hàng thất bại: " + resp.body);
            ctx.setCreatedAccountId(null);
            result.pass("Xóa khách hàng thành công");
        });

        // ═══ WAREHOUSE TESTS ═════════════════════════════════════════════════════
        // Endpoint: /api/warehouse/ (SINGULAR — không phải /warehouses/)
        // WarehouseCreate: ma_kho(str,req), ten_kho(str,req), dia_chi(str,req), ma_sp(str,req), gia_nhap, so_luong, trang_thai

        runTest("GET /api/warehouse/ - Danh sách kho hàng", result -> {
            ApiClient.ApiResponse resp = api.get("/api/warehouse/");
            assertTrue(resp.success, "Lỗi lấy kho hàng: " + resp.statusCode);
            result.pass("Danh sách kho hàng (status " + resp.statusCode + ")");
        });

        runTest("POST /api/warehouse/ - Nhập kho hàng mới", result -> {
            // Cần ma_sp hợp lệ — lấy từ danh sách sản phẩm
            ApiClient.ApiResponse pResp = api.get("/api/products/");
            String maSp = null;
            if (pResp.success) {
                JsonArray products = pResp.jsonObject().getAsJsonArray("products");
                if (products != null && products.size() > 0) {
                    JsonObject p = products.get(0).getAsJsonObject();
                    maSp = p.has("ma_sp") && !p.get("ma_sp").isJsonNull()
                           ? p.get("ma_sp").getAsString() : null;
                }
            }
            if (maSp == null) {
                result.skip("Không có sản phẩm nào để tạo kho");
                return;
            }
            String body = String.format("""
                {
                    "ma_kho": "%sKHO%d",
                    "ten_kho": "%sKho Test %d",
                    "dia_chi": "Khu Công Nghiệp Test",
                    "ma_sp": "%s",
                    "gia_nhap": 450000,
                    "so_luong": 50,
                    "trang_thai": "Hoạt động"
                }
                """, prefix, ts, prefix, ts, maSp);
            ApiClient.ApiResponse resp = api.post("/api/warehouse/", body);
            assertTrue(resp.success, "Nhập kho thất bại: " + resp.body);
            int whId = resp.getIntField("id");
            assertTrue(whId > 0, "id kho không hợp lệ: " + whId);
            ctx.setCreatedWarehouseId(whId);
            result.pass("Kho hàng tạo thành công, id=" + whId + ", ma_sp=" + maSp);
        });

        runTest("GET /api/warehouse/{id} - Lấy kho hàng theo ID", result -> {
            Integer whId = ctx.getCreatedWarehouseId();
            if (whId == null) { result.skip("Chưa tạo kho hàng"); return; }
            ApiClient.ApiResponse resp = api.get("/api/warehouse/" + whId);
            assertTrue(resp.success, "Lấy kho thất bại: " + resp.body);
            String maKho = resp.getField("ma_kho");
            result.pass("Kho hàng: ma_kho=" + maKho + ", so_luong=" + resp.getField("so_luong"));
        });

        // ═══ AREA TESTS ══════════════════════════════════════════════════════════
        // AreaCreate: name, code, type, province (required), manager, status, priority

        runTest("GET /api/areas/ - Danh sách khu vực", result -> {
            ApiClient.ApiResponse resp = api.get("/api/areas/");
            assertTrue(resp.success, "Lỗi lấy khu vực: " + resp.statusCode);
            result.pass("Danh sách khu vực (status " + resp.statusCode + ")");
        });

        runTest("POST /api/areas/ - Tạo khu vực mới", result -> {
            String body = String.format("""
                {
                    "name": "%sKhu Vực %d",
                    "code": "%sKV%d",
                    "type": "Tỉnh",
                    "province": "Hồ Chí Minh",
                    "manager": "Quản Lý Test",
                    "status": "active",
                    "priority": "medium"
                }
                """, prefix, ts, prefix, ts % 10000);
            ApiClient.ApiResponse resp = api.post("/api/areas/", body);
            assertTrue(resp.success, "Tạo khu vực thất bại: " + resp.body);
            int areaId = resp.getIntField("id");
            assertTrue(areaId > 0, "id khu vực không hợp lệ");
            ctx.setCreatedAreaId(areaId);
            result.pass("Khu vực tạo thành công, id=" + areaId);
        });

        // ═══ SHOP TESTS ══════════════════════════════════════════════════════════
        // ShopCreate: name, code, area_id, address (required)

        runTest("GET /api/shops/ - Danh sách cửa hàng", result -> {
            ApiClient.ApiResponse resp = api.get("/api/shops/");
            assertTrue(resp.success, "Lỗi lấy cửa hàng: " + resp.statusCode);
            result.pass("Danh sách cửa hàng (status " + resp.statusCode + ")");
        });

        runTest("POST /api/shops/ - Tạo cửa hàng mới", result -> {
            Integer areaId = ctx.getCreatedAreaId();
            if (areaId == null) { result.skip("Chưa tạo khu vực để liên kết"); return; }
            String body = String.format("""
                {
                    "name": "%sCửa Hàng %d",
                    "code": "%sCH%d",
                    "area_id": %d,
                    "address": "123 Đường Test %d",
                    "phone": "028%d",
                    "email": "shop%d@postest.com",
                    "manager": "Manager Test",
                    "status": "active"
                }
                """, prefix, ts, prefix, ts % 10000, areaId, ts, ts % 10000000, ts);
            ApiClient.ApiResponse resp = api.post("/api/shops/", body);
            assertTrue(resp.success, "Tạo cửa hàng thất bại: " + resp.body);
            int shopId = resp.getIntField("id");
            assertTrue(shopId > 0, "id cửa hàng không hợp lệ");
            ctx.setCreatedShopId(shopId);
            result.pass("Cửa hàng tạo thành công, id=" + shopId);
        });

        // ═══ PRICES CRUD ══════════════════════════════════════════════════════════
        // PriceCreate: ma_sp(str), ten_sp(str), gia_chung(float), ghi_chu(str)

        runTest("GET /api/prices/ - Danh sách bảng giá", result -> {
            ApiClient.ApiResponse resp = api.get("/api/prices/");
            assertTrue(resp.success, "Lỗi lấy bảng giá: " + resp.statusCode);
            result.pass("Bảng giá (status " + resp.statusCode + ")");
        });

        runTest("POST /api/prices/ - Tạo bảng giá mới", result -> {
            // Lấy mã sản phẩm để liên kết
            ApiClient.ApiResponse pResp = api.get("/api/products/");
            String maSp = "TEST_PRICE_" + ts;
            String tenSp = "Sản phẩm Test Giá";
            if (pResp.success) {
                JsonArray products = pResp.jsonObject().getAsJsonArray("products");
                if (products != null && products.size() > 0) {
                    JsonObject p = products.get(0).getAsJsonObject();
                    if (p.has("ma_sp") && !p.get("ma_sp").isJsonNull())
                        maSp = p.get("ma_sp").getAsString();
                    if (p.has("ten_sp") && !p.get("ten_sp").isJsonNull())
                        tenSp = p.get("ten_sp").getAsString();
                }
            }
            String body = String.format("""
                {
                    "ma_sp": "%s",
                    "ten_sp": "%s",
                    "gia_chung": 850000,
                    "ghi_chu": "Bảng giá test từ POSTestApp"
                }
                """, maSp, tenSp);
            ApiClient.ApiResponse resp = api.post("/api/prices/", body);
            assertTrue(resp.success, "Tạo bảng giá thất bại: " + resp.body);
            int priceId = resp.getIntField("id");
            assertTrue(priceId > 0, "id bảng giá không hợp lệ: " + priceId);
            ctx.set("createdPriceId", priceId);
            result.pass("Bảng giá tạo thành công, id=" + priceId);
        });

        runTest("PUT /api/prices/{id} - Cập nhật bảng giá", result -> {
            Integer priceId = ctx.getInteger("createdPriceId");
            if (priceId == null) { result.skip("Chưa tạo bảng giá"); return; }
            String body = """
                {"gia_chung": 950000, "ghi_chu": "Đã cập nhật bởi POSTestApp"}
                """;
            ApiClient.ApiResponse resp = api.put("/api/prices/" + priceId, body);
            assertTrue(resp.success, "Cập nhật bảng giá thất bại: " + resp.body);
            result.pass("Bảng giá cập nhật thành công, id=" + priceId);
        });

        runTest("DELETE /api/prices/{id} - Xóa bảng giá", result -> {
            Integer priceId = ctx.getInteger("createdPriceId");
            if (priceId == null) { result.skip("Chưa tạo bảng giá để xóa"); return; }
            ApiClient.ApiResponse resp = api.delete("/api/prices/" + priceId);
            assertTrue(resp.success, "Xóa bảng giá thất bại: " + resp.body);
            ctx.set("createdPriceId", null);
            result.pass("Bảng giá xóa thành công");
        });

        // ═══ SCHEDULES CRUD ══════════════════════════════════════════════════════
        // ScheduleCreate: employee_id(int), work_date(date), shift_type(str), notes(str)

        runTest("GET /api/schedules/ - Danh sách lịch làm việc", result -> {
            ApiClient.ApiResponse resp = api.get("/api/schedules/");
            assertTrue(resp.success, "Lỗi lấy lịch làm việc: " + resp.statusCode);
            result.pass("Lịch làm việc (status " + resp.statusCode + ")");
        });

        runTest("POST /api/schedules/ - Tạo lịch làm việc (cần employee_id hợp lệ)", result -> {
            // Lấy danh sách nhân viên để lấy employee_id
            ApiClient.ApiResponse usersResp = api.get("/api/users/");
            int empId = -1;
            if (usersResp.success) {
                com.google.gson.JsonArray users = usersResp.jsonObject().getAsJsonArray("users");
                if (users == null) {
                    // Try as array directly
                    if (usersResp.json.isJsonArray()) {
                        users = usersResp.json.getAsJsonArray();
                    }
                }
                if (users != null && users.size() > 0) {
                    JsonObject u = users.get(0).getAsJsonObject();
                    empId = u.has("id") ? u.get("id").getAsInt() : -1;
                }
            }
            if (empId <= 0) {
                result.skip("Không tìm thấy nhân viên hợp lệ để tạo lịch");
                return;
            }
            String body = String.format("""
                {
                    "employee_id": %d,
                    "work_date": "2026-07-15",
                    "shift_type": "Ca sáng",
                    "notes": "Lịch test từ POSTestApp"
                }
                """, empId);
            ApiClient.ApiResponse resp = api.post("/api/schedules/", body);
            assertTrue(resp.success, "Tạo lịch làm việc thất bại: " + resp.body);
            int schedId = resp.getIntField("id");
            assertTrue(schedId > 0, "id lịch không hợp lệ: " + schedId);
            ctx.set("createdScheduleId", schedId);
            result.pass("Lịch làm việc tạo thành công, id=" + schedId + " cho employee_id=" + empId);
        });

        runTest("PUT /api/schedules/{id} - Cập nhật lịch làm việc", result -> {
            Integer schedId = ctx.getInteger("createdScheduleId");
            if (schedId == null) { result.skip("Chưa tạo lịch làm việc"); return; }
            String body = """
                {"shift_type": "Ca chiều", "notes": "Đã cập nhật bởi POSTestApp"}
                """;
            ApiClient.ApiResponse resp = api.put("/api/schedules/" + schedId, body);
            assertTrue(resp.success, "Cập nhật lịch thất bại: " + resp.body);
            result.pass("Lịch làm việc cập nhật thành công");
        });

        runTest("DELETE /api/schedules/{id} - Xóa lịch làm việc", result -> {
            Integer schedId = ctx.getInteger("createdScheduleId");
            if (schedId == null) { result.skip("Chưa tạo lịch để xóa"); return; }
            ApiClient.ApiResponse resp = api.delete("/api/schedules/" + schedId);
            assertTrue(resp.success, "Xóa lịch thất bại: " + resp.body);
            ctx.set("createdScheduleId", null);
            result.pass("Lịch làm việc xóa thành công");
        });

        // Cleanup
        runTest("Cleanup: Xóa cửa hàng và khu vực test", result -> {
            StringBuilder cleaned = new StringBuilder();
            Integer shopId = ctx.getCreatedShopId();
            if (shopId != null) {
                if (api.delete("/api/shops/" + shopId).success) {
                    cleaned.append("shop "); ctx.setCreatedShopId(null);
                }
            }
            Integer areaId = ctx.getCreatedAreaId();
            if (areaId != null) {
                if (api.delete("/api/areas/" + areaId).success) {
                    cleaned.append("area"); ctx.setCreatedAreaId(null);
                }
            }
            Integer whId = ctx.getCreatedWarehouseId();
            if (whId != null) {
                if (api.delete("/api/warehouse/" + whId).success) {
                    cleaned.append(" warehouse"); ctx.setCreatedWarehouseId(null);
                }
            }
            result.pass("Đã xóa: " + (cleaned.length() > 0 ? cleaned.toString() : "không có gì"));
        });

        return results;
    }
}
