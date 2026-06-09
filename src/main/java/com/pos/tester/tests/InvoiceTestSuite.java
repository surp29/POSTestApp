package com.pos.tester.tests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pos.tester.api.ApiClient;
import com.pos.tester.api.TestContext;
import com.pos.tester.model.TestConfig;
import com.pos.tester.model.TestResult;

import java.time.LocalDate;
import java.util.List;

public class InvoiceTestSuite extends BaseTestSuite {
    private final TestConfig config;

    public InvoiceTestSuite(TestContext ctx, TestConfig config) {
        super(ctx);
        this.config = config;
    }

    @Override public String getModuleName() { return "Invoices"; }
    @Override public String getDescription() { return "Invoice CRUD, Stock deduction, General Diary logging"; }

    @Override
    public List<TestResult> runAll() throws InterruptedException {
        results.clear();
        // Fields: so_hd, ngay_hd, nguoi_mua, tong_tien, trang_thai, hinh_thuc_tt, items[]
        // Items:  product_id, product_code, product_name, so_luong, don_gia, total_price

        final int[]    productId   = {-1};
        final int[]    initialQty  = {0};
        final String[] productCode = {null};
        final String[] productName = {null};
        final String[] soHd        = {null};

        // Setup
        runTest("Setup: Tìm sản phẩm đủ tồn kho (so_luong >= 5)", result -> {
            ApiClient.ApiResponse resp = api.get("/api/products/");
            assertTrue(resp.success, "Không lấy được sản phẩm");
            JsonArray products = resp.jsonObject().getAsJsonArray("products");
            if (products == null) { result.skip("Không có sản phẩm"); return; }
            for (int i = 0; i < products.size(); i++) {
                JsonObject p = products.get(i).getAsJsonObject();
                int qty = p.has("so_luong") ? p.get("so_luong").getAsInt() : 0;
                if (qty >= 5) {
                    productId[0]   = p.get("id").getAsInt();
                    initialQty[0]  = qty;
                    productCode[0] = p.has("ma_sp")  ? p.get("ma_sp").getAsString()  : "";
                    productName[0] = p.has("ten_sp") ? p.get("ten_sp").getAsString() : "Sản phẩm";
                    break;
                }
            }
            if (productId[0] == -1) {
                result.skip("Không có sản phẩm nào có so_luong >= 5");
            } else {
                result.pass("Dùng sản phẩm id=" + productId[0] + " ma=" + productCode[0] + " qty=" + initialQty[0]);
            }
        });

        // 1. List invoices
        runTest("GET /api/invoices/ - Danh sách hóa đơn", result -> {
            ApiClient.ApiResponse resp = api.get("/api/invoices/");
            assertTrue(resp.success, "Lỗi lấy hóa đơn: " + resp.statusCode);
            result.pass("Hóa đơn lấy thành công (status " + resp.statusCode + ")");
        });

        // 2. Get next invoice number
        runTest("GET /api/invoices/next-number - Lấy số hóa đơn tiếp theo", result -> {
            ApiClient.ApiResponse resp = api.get("/api/invoices/next-number");
            assertTrue(resp.success, "Lấy số HĐ thất bại: " + resp.statusCode);
            String full = resp.getField("full_invoice_number");
            assertNotNull(full, "full_invoice_number");
            soHd[0] = full;
            result.pass("Số hóa đơn tiếp theo: " + full);
        });

        // 3. Create invoice
        runTest("POST /api/invoices/ - Tạo hóa đơn (kích hoạt trừ tồn kho)", result -> {
            if (productId[0] == -1) { result.skip("Không có sản phẩm"); return; }
            if (soHd[0] == null) soHd[0] = "HD-TEST-" + System.currentTimeMillis() % 10000;
            double donGia    = 750000;
            int    soLuong   = 2;
            double totalItem = donGia * soLuong;
            String body = String.format("""
                {
                    "so_hd": "%s",
                    "ngay_hd": "%s",
                    "nguoi_mua": "Khách Hàng Test",
                    "tong_tien": %.0f,
                    "trang_thai": "Đã thanh toán",
                    "hinh_thuc_tt": "Tiền mặt",
                    "items": [
                        {
                            "product_id": %d,
                            "product_code": "%s",
                            "product_name": "%s",
                            "so_luong": %d,
                            "don_gia": %.0f,
                            "total_price": %.0f
                        }
                    ]
                }
                """, soHd[0], LocalDate.now().toString(), totalItem,
                productId[0], productCode[0], productName[0],
                soLuong, donGia, totalItem);
            ApiClient.ApiResponse resp = api.post("/api/invoices/", body);
            assertTrue(resp.success, "Tạo hóa đơn thất bại: " + resp.body);
            int invoiceId = resp.getIntField("id");
            assertTrue(invoiceId > 0, "id hóa đơn không hợp lệ");
            ctx.setCreatedInvoiceId(invoiceId);
            result.pass("Hóa đơn tạo thành công, id=" + invoiceId + ", so_hd=" + soHd[0]);
        });

        // 4. Verify stock deducted
        runTest("Kiểm tra tồn kho bị trừ sau tạo hóa đơn", result -> {
            if (productId[0] == -1 || ctx.getCreatedInvoiceId() == null) {
                result.skip("Chưa có hóa đơn để kiểm tra");
                return;
            }
            ApiClient.ApiResponse resp = api.get("/api/products/" + productId[0]);
            assertTrue(resp.success, "Không lấy được sản phẩm sau hóa đơn");
            int newQty = resp.getIntField("so_luong");
            int expected = initialQty[0] - 2;
            assertTrue(newQty == expected,
                "Tồn kho chưa trừ đúng: expected=" + expected + " got=" + newQty);
            result.pass("Tồn kho trừ đúng: " + initialQty[0] + " → " + newQty);
        });

        // 5. Get invoice by ID — returns InvoiceOut: so_hd, nguoi_mua, tong_tien, trang_thai
        runTest("GET /api/invoices/{id} - Lấy hóa đơn theo ID", result -> {
            Integer invoiceId = ctx.getCreatedInvoiceId();
            if (invoiceId == null) { result.skip("Chưa tạo hóa đơn"); return; }
            ApiClient.ApiResponse resp = api.get("/api/invoices/" + invoiceId);
            assertTrue(resp.success, "Lấy hóa đơn thất bại: " + resp.body);
            String soHdRes   = resp.getField("so_hd");
            String tongTien  = resp.getField("tong_tien");
            assertNotNull(soHdRes, "so_hd");
            result.pass("Hóa đơn: so_hd=" + soHdRes + ", tong_tien=" + tongTien);
        });

        // 6. Get invoice details (items)
        runTest("GET /api/invoices/{id}/details - Chi tiết hóa đơn", result -> {
            Integer invoiceId = ctx.getCreatedInvoiceId();
            if (invoiceId == null) { result.skip("Chưa tạo hóa đơn"); return; }
            ApiClient.ApiResponse resp = api.get("/api/invoices/" + invoiceId + "/details");
            assertTrue(resp.success, "Lấy chi tiết thất bại: " + resp.statusCode);
            result.pass("Chi tiết hóa đơn lấy thành công (status " + resp.statusCode + ")");
        });

        // 7. General diary logged
        runTest("Kiểm tra General Diary ghi nhận sau hóa đơn", result -> {
            if (ctx.getCreatedInvoiceId() == null) { result.skip("Chưa tạo hóa đơn"); return; }
            ApiClient.ApiResponse resp = api.get("/api/general-diary/");
            assertTrue(resp.success, "Không lấy được general diary: " + resp.statusCode);
            result.pass("General Diary truy cập thành công sau tạo hóa đơn");
        });

        return results;
    }
}
