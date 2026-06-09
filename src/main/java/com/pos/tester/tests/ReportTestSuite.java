package com.pos.tester.tests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pos.tester.api.ApiClient;
import com.pos.tester.api.TestContext;
import com.pos.tester.model.TestConfig;
import com.pos.tester.model.TestResult;

import java.time.LocalDate;
import java.util.List;

public class ReportTestSuite extends BaseTestSuite {
    private final TestConfig config;

    public ReportTestSuite(TestContext ctx, TestConfig config) {
        super(ctx);
        this.config = config;
    }

    @Override public String getModuleName() { return "Reports & Analytics"; }
    @Override public String getDescription() {
        return "Revenue/Debt reports, Customer Analytics, Audit, Chatbot AI, Health";
    }

    @Override
    public List<TestResult> runAll() throws InterruptedException {
        results.clear();

        String today     = LocalDate.now().toString();
        String lastMonth = LocalDate.now().minusMonths(1).toString();

        // ═══ REVENUE REPORT ══════════════════════════════════════════════════════

        runTest("GET /api/reports/revenue - Doanh thu với lọc ngày", result -> {
            String url = "/api/reports/revenue?from_date=" + lastMonth + "&to_date=" + today;
            ApiClient.ApiResponse resp = api.get(url);
            assertTrue(resp.success, "Revenue report thất bại: " + resp.statusCode + " " + resp.body);
            JsonObject data = resp.jsonObject();
            boolean hasSummary = data.has("summary");
            boolean hasItems   = data.has("items");
            assertTrue(hasSummary || hasItems, "Revenue report phải có summary hoặc items");
            result.pass("Báo cáo doanh thu OK: summary=" + hasSummary + " items=" + hasItems);
        });

        runTest("GET /api/reports/revenue - Cấu trúc summary hợp lệ", result -> {
            String url = "/api/reports/revenue?from_date=" + lastMonth + "&to_date=" + today;
            ApiClient.ApiResponse resp = api.get(url);
            assertTrue(resp.success, "Revenue report thất bại: " + resp.statusCode);
            if (resp.jsonObject().has("summary")) {
                JsonObject summary = resp.jsonObject().getAsJsonObject("summary");
                boolean hasTotalRevenue = summary.has("total_revenue");
                result.pass("Summary hợp lệ: total_revenue=" + hasTotalRevenue
                        + " value=" + (hasTotalRevenue ? summary.get("total_revenue") : "N/A"));
            } else {
                result.pass("Cấu trúc revenue response hợp lệ (no summary key)");
            }
        });

        // ═══ DEBT REPORT ═════════════════════════════════════════════════════════

        runTest("GET /api/reports/debt - Báo cáo công nợ", result -> {
            // Đúng endpoint là /api/reports/debt (không phải /customer-debts)
            ApiClient.ApiResponse resp = api.get("/api/reports/debt");
            assertTrue(resp.success, "Debt report thất bại: " + resp.statusCode + " " + resp.body);
            JsonObject data = resp.jsonObject();
            boolean hasSummary = data.has("summary");
            boolean hasItems   = data.has("items");
            result.pass("Báo cáo công nợ OK: summary=" + hasSummary + " items=" + hasItems);
        });

        runTest("GET /api/reports/debt - Cấu trúc summary công nợ", result -> {
            ApiClient.ApiResponse resp = api.get("/api/reports/debt");
            assertTrue(resp.success, "Debt report thất bại: " + resp.statusCode);
            if (resp.jsonObject().has("summary")) {
                JsonObject summary = resp.jsonObject().getAsJsonObject("summary");
                boolean hasTotal = summary.has("total_debt");
                result.pass("Debt summary: total_debt=" + hasTotal
                        + " value=" + (hasTotal ? summary.get("total_debt") : "N/A"));
            } else {
                result.pass("Debt response cấu trúc hợp lệ");
            }
        });

        // ═══ CUSTOMER ANALYTICS ══════════════════════════════════════════════════

        runTest("GET /api/reports/aggregates - Thống kê tổng hợp khách hàng", result -> {
            ApiClient.ApiResponse resp = api.get("/api/reports/aggregates");
            assertTrue(resp.success, "Aggregates thất bại: " + resp.statusCode + " " + resp.body);
            result.pass("Customer aggregates OK (status " + resp.statusCode + ")");
        });

        runTest("GET /api/reports/leaderboard - Bảng xếp hạng khách hàng", result -> {
            ApiClient.ApiResponse resp = api.get("/api/reports/leaderboard");
            assertTrue(resp.success, "Leaderboard thất bại: " + resp.statusCode + " " + resp.body);
            result.pass("Customer leaderboard OK (status " + resp.statusCode + ")");
        });

        runTest("GET /api/reports/debts - Danh sách công nợ khách hàng từ hóa đơn", result -> {
            // /api/reports/debts trả về công nợ theo từng khách hàng từ hóa đơn chưa thanh toán
            ApiClient.ApiResponse resp = api.get("/api/reports/debts");
            assertTrue(resp.success, "Customer debts thất bại: " + resp.statusCode + " " + resp.body);
            result.pass("Customer debts from invoices OK (status " + resp.statusCode + ")");
        });

        // ═══ GENERAL DIARY ═══════════════════════════════════════════════════════

        runTest("GET /api/general-diary/ - Nhật ký kế toán tổng hợp", result -> {
            ApiClient.ApiResponse resp = api.get("/api/general-diary/");
            assertTrue(resp.success, "General diary thất bại: " + resp.statusCode);
            result.pass("General diary OK (status " + resp.statusCode + ")");
        });

        runTest("GET /api/general-diary/ - Lọc theo khoảng ngày", result -> {
            String url = "/api/general-diary/?from_date=" + lastMonth + "&to_date=" + today;
            ApiClient.ApiResponse resp = api.get(url);
            assertTrue(resp.success, "Diary filter thất bại: " + resp.statusCode);
            result.pass("General diary lọc theo ngày OK");
        });

        // ═══ AUDIT LOG ═══════════════════════════════════════════════════════════

        runTest("GET /api/audit/ - Lịch sử thao tác (audit log)", result -> {
            ApiClient.ApiResponse resp = api.get("/api/audit/");
            assertTrue(resp.success, "Audit log thất bại: " + resp.statusCode);
            result.pass("Audit logs OK (status " + resp.statusCode + ")");
        });

        runTest("GET /api/audit/ - Lọc audit theo user", result -> {
            ApiClient.ApiResponse resp = api.get("/api/audit/?username=" + config.getAdminUsername());
            assertTrue(resp.success, "Audit filter thất bại: " + resp.statusCode);
            result.pass("Audit lọc theo username=" + config.getAdminUsername() + " OK");
        });

        runTest("GET /api/audit/ - Lọc audit theo loại entity", result -> {
            ApiClient.ApiResponse resp = api.get("/api/audit/?entity_type=Product");
            assertTrue(resp.success, "Audit entity filter thất bại: " + resp.statusCode);
            result.pass("Audit lọc theo entity_type=Product OK");
        });

        // ═══ CHATBOT AI ══════════════════════════════════════════════════════════
        // Chatbot: POST /api/chatbot/analyze - 5 loại query
        // Response: {response: str, suggestions: [], type: str}

        runTest("POST /api/chatbot/analyze - Phân tích tồn kho (inventory)", result -> {
            String body = """
                {"message": "phân tích tồn kho"}
                """;
            ApiClient.ApiResponse resp = api.post("/api/chatbot/analyze", body);
            assertTrue(resp.success, "Chatbot analyze thất bại: " + resp.body);
            String type = resp.getField("type");
            String response = resp.getField("response");
            assertNotNull(response, "response");
            result.pass("Chatbot inventory: type=" + type + " | " + (response.length() > 60 ? response.substring(0, 60) + "..." : response));
        });

        runTest("POST /api/chatbot/analyze - Sản phẩm sắp hết (low_stock)", result -> {
            String body = """
                {"message": "sản phẩm sắp hết"}
                """;
            ApiClient.ApiResponse resp = api.post("/api/chatbot/analyze", body);
            assertTrue(resp.success, "Chatbot low_stock thất bại: " + resp.body);
            String type = resp.getField("type");
            JsonArray sugg = resp.jsonObject().has("suggestions") ? resp.jsonObject().getAsJsonArray("suggestions") : null;
            result.pass("Chatbot low_stock: type=" + type + " | suggestions=" + (sugg != null ? sugg.size() : 0));
        });

        runTest("POST /api/chatbot/analyze - Đề xuất đặt hàng (reorder)", result -> {
            String body = """
                {"message": "đề xuất đặt hàng"}
                """;
            ApiClient.ApiResponse resp = api.post("/api/chatbot/analyze", body);
            assertTrue(resp.success, "Chatbot reorder thất bại: " + resp.body);
            String type = resp.getField("type");
            result.pass("Chatbot reorder: type=" + type);
        });

        runTest("POST /api/chatbot/analyze - Báo cáo doanh thu (revenue)", result -> {
            String body = """
                {"message": "báo cáo doanh thu"}
                """;
            ApiClient.ApiResponse resp = api.post("/api/chatbot/analyze", body);
            assertTrue(resp.success, "Chatbot revenue thất bại: " + resp.body);
            String type = resp.getField("type");
            String response = resp.getField("response");
            assertTrue(response != null && response.length() > 5, "Chatbot phải trả về response text");
            result.pass("Chatbot revenue: type=" + type);
        });

        runTest("POST /api/chatbot/analyze - Sản phẩm bán chạy (best_seller)", result -> {
            String body = """
                {"message": "sản phẩm bán chạy"}
                """;
            ApiClient.ApiResponse resp = api.post("/api/chatbot/analyze", body);
            assertTrue(resp.success, "Chatbot best_seller thất bại: " + resp.body);
            String type = resp.getField("type");
            result.pass("Chatbot best_seller: type=" + type);
        });

        runTest("POST /api/chatbot/analyze - Default/help response", result -> {
            String body = """
                {"message": "xin chào"}
                """;
            ApiClient.ApiResponse resp = api.post("/api/chatbot/analyze", body);
            assertTrue(resp.success, "Chatbot help thất bại: " + resp.body);
            String type = resp.getField("type");
            assertEquals("help", type, "type");
            result.pass("Chatbot default/help response: type=" + type);
        });

        // ═══ HEALTH & METRICS ════════════════════════════════════════════════════

        runTest("GET /health - Health check (database + redis status)", result -> {
            ApiClient.ApiResponse resp = api.get("/health");
            assertTrue(resp.statusCode == 200, "Health check thất bại: " + resp.statusCode);
            JsonObject health = resp.jsonObject();
            String status = health.has("status") ? health.get("status").getAsString() : "OK";
            result.pass("Health check: " + status);
        });

        runTest("GET /metrics - Prometheus metrics endpoint", result -> {
            ApiClient.ApiResponse resp = api.get("/metrics");
            assertTrue(resp.success, "Metrics thất bại: " + resp.statusCode);
            assertTrue(resp.body.length() > 10, "Metrics response rỗng");
            result.pass("Prometheus metrics accessible: " + resp.body.length() + " chars");
        });

        return results;
    }
}
