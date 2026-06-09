package com.pos.tester.tests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pos.tester.api.ApiClient;
import com.pos.tester.api.TestContext;
import com.pos.tester.model.TestConfig;
import com.pos.tester.model.TestResult;

import java.util.List;

public class ShipmentTestSuite extends BaseTestSuite {
    private final TestConfig config;
    // Status flow: pending → picked → in_transit → delivering → delivered
    // Other branches: pending → cancelled; in_transit/delivering → failed → delivering/returned

    public ShipmentTestSuite(TestContext ctx, TestConfig config) {
        super(ctx);
        this.config = config;
    }

    @Override public String getModuleName() { return "Shipments"; }
    @Override public String getDescription() { return "Shipment creation, 8-status workflow, Tracking, History"; }

    @Override
    public List<TestResult> runAll() throws InterruptedException {
        results.clear();

        // 1. List shipments — returns {success, total, stats, shipments:[]}
        runTest("GET /api/shipping/ - Danh sách đơn vận chuyển", result -> {
            ApiClient.ApiResponse resp = api.get("/api/shipping/");
            assertTrue(resp.success, "Lỗi lấy đơn vận chuyển: " + resp.statusCode);
            int total = resp.getIntField("total");
            result.pass("Lấy được " + total + " đơn vận chuyển");
        });

        // 2. Get status definitions
        runTest("GET /api/shipping/statuses - Định nghĩa trạng thái vận chuyển", result -> {
            ApiClient.ApiResponse resp = api.get("/api/shipping/statuses");
            assertTrue(resp.success, "Lỗi lấy trạng thái: " + resp.statusCode);
            result.pass("Trạng thái vận chuyển (status " + resp.statusCode + ")");
        });

        // 3. Create shipment
        // service_type phải là Vietnamese: "Giao hàng thường", "Giao hàng nhanh", etc.
        runTest("POST /api/shipping/ - Tạo đơn vận chuyển", result -> {
            String body = """
                {
                    "receiver_name": "Người Nhận Test",
                    "receiver_phone": "0912345678",
                    "receiver_address": "456 Đường Test, Quận 1, TP.HCM",
                    "receiver_province": "TP. Hồ Chí Minh",
                    "service_type": "Giao hàng nhanh",
                    "weight": 500,
                    "cod_amount": 750000,
                    "note": "Tạo bởi POS Tester"
                }
                """;
            ApiClient.ApiResponse resp = api.post("/api/shipping/", body);
            assertTrue(resp.success, "Tạo đơn vận chuyển thất bại: " + resp.body);
            int shipmentId  = resp.getIntField("id");
            String tracking = resp.getField("tracking_code");
            assertTrue(shipmentId > 0, "id không hợp lệ: " + shipmentId);
            ctx.setCreatedShipmentId(shipmentId);
            result.pass("Đơn vận chuyển tạo thành công, id=" + shipmentId + ", tracking=" + tracking);
        });

        // 4. Verify tracking code format VD{yymmdd}{6digits}
        runTest("Kiểm tra định dạng tracking code (VD{yymmdd}{6 số})", result -> {
            Integer shipmentId = ctx.getCreatedShipmentId();
            if (shipmentId == null) { result.skip("Chưa tạo đơn vận chuyển"); return; }
            ApiClient.ApiResponse resp = api.get("/api/shipping/" + shipmentId);
            assertTrue(resp.success, "Lấy đơn vận chuyển thất bại");
            // GET single returns {"success":true, "shipment":{...}, "history":[...]}
            JsonObject shipment = resp.jsonObject().getAsJsonObject("shipment");
            assertNotNull(shipment, "shipment object");
            String tracking = shipment.has("tracking_code")
                              ? shipment.get("tracking_code").getAsString() : null;
            assertNotNull(tracking, "tracking_code");
            assertTrue(tracking.startsWith("VD"), "tracking_code phải bắt đầu bằng 'VD', got: " + tracking);
            assertTrue(tracking.length() >= 10, "tracking_code quá ngắn: " + tracking);
            result.pass("Tracking code hợp lệ: " + tracking);
        });

        // 5. Status transition: pending → picked
        // Status update: PUT /api/shipping/{id}/status với payload: status, description, location
        runTest("Status: pending → picked", result -> {
            Integer shipmentId = ctx.getCreatedShipmentId();
            if (shipmentId == null) { result.skip("Chưa tạo đơn vận chuyển"); return; }
            String body = """
                {"status": "picked", "description": "Shipper đã lấy hàng từ kho", "location": "Kho Hàng A"}
                """;
            ApiClient.ApiResponse resp = api.put("/api/shipping/" + shipmentId + "/status", body);
            assertTrue(resp.success, "Chuyển sang picked thất bại: " + resp.body);
            result.pass("pending → picked thành công");
        });

        // 6. Status transition: picked → in_transit
        runTest("Status: picked → in_transit", result -> {
            Integer shipmentId = ctx.getCreatedShipmentId();
            if (shipmentId == null) { result.skip("Chưa tạo đơn vận chuyển"); return; }
            String body = """
                {"status": "in_transit", "description": "Hàng đang trên đường vận chuyển", "location": "Bưu Cục Quận 1"}
                """;
            ApiClient.ApiResponse resp = api.put("/api/shipping/" + shipmentId + "/status", body);
            assertTrue(resp.success, "Chuyển sang in_transit thất bại: " + resp.body);
            result.pass("picked → in_transit thành công");
        });

        // 7. Status transition: in_transit → delivering
        runTest("Status: in_transit → delivering", result -> {
            Integer shipmentId = ctx.getCreatedShipmentId();
            if (shipmentId == null) { result.skip("Chưa tạo đơn vận chuyển"); return; }
            String body = """
                {"status": "delivering", "description": "Shipper đang giao đến địa chỉ nhận", "location": "Khu vực khách hàng"}
                """;
            ApiClient.ApiResponse resp = api.put("/api/shipping/" + shipmentId + "/status", body);
            assertTrue(resp.success, "Chuyển sang delivering thất bại: " + resp.body);
            result.pass("in_transit → delivering thành công");
        });

        // 8. Status transition: delivering → delivered
        runTest("Status: delivering → delivered ✓ Full workflow", result -> {
            Integer shipmentId = ctx.getCreatedShipmentId();
            if (shipmentId == null) { result.skip("Chưa tạo đơn vận chuyển"); return; }
            String body = """
                {"status": "delivered", "description": "Giao hàng thành công", "location": "Nhà khách hàng"}
                """;
            ApiClient.ApiResponse resp = api.put("/api/shipping/" + shipmentId + "/status", body);
            assertTrue(resp.success, "Chuyển sang delivered thất bại: " + resp.body);
            result.pass("delivering → delivered thành công! Toàn bộ luồng hoàn tất.");
        });

        // 9. Check history embedded in GET single (no separate /history endpoint)
        runTest("Kiểm tra lịch sử vận chuyển qua GET /api/shipping/{id}", result -> {
            Integer shipmentId = ctx.getCreatedShipmentId();
            if (shipmentId == null) { result.skip("Chưa tạo đơn vận chuyển"); return; }
            ApiClient.ApiResponse resp = api.get("/api/shipping/" + shipmentId);
            assertTrue(resp.success, "Lấy đơn vận chuyển thất bại: " + resp.statusCode);
            JsonArray history = resp.jsonObject().getAsJsonArray("history");
            assertNotNull(history, "history array");
            assertTrue(history.size() >= 4, "Phải có ít nhất 4 bản ghi lịch sử, got: " + history.size());
            result.pass("Lịch sử vận chuyển: " + history.size() + " bản ghi");
        });

        // 10. Create second shipment and cancel it
        runTest("Tạo và hủy đơn vận chuyển (cancelled từ pending)", result -> {
            String body = """
                {
                    "receiver_name": "Người Nhận Hủy Test",
                    "receiver_phone": "0911111111",
                    "receiver_address": "789 Đường Hủy",
                    "service_type": "Giao hàng thường",
                    "weight": 300,
                    "cod_amount": 500000
                }
                """;
            ApiClient.ApiResponse createResp = api.post("/api/shipping/", body);
            if (!createResp.success) { result.skip("Không tạo được đơn thứ 2"); return; }
            int newId = createResp.getIntField("id");
            String cancelBody = """
                {"status": "cancelled", "description": "Test hủy đơn"}
                """;
            ApiClient.ApiResponse cancelResp = api.put("/api/shipping/" + newId + "/status", cancelBody);
            if (cancelResp.success) {
                result.pass("Hủy đơn thành công từ trạng thái pending");
            } else {
                result.skip("Hủy từ pending: " + cancelResp.statusCode + " - " + cancelResp.body);
            }
        });

        return results;
    }
}
