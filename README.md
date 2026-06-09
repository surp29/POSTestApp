# POS System Test Tool v2.0

Ứng dụng desktop Java để test **API backend** và **UI frontend** của hệ thống POS.

## Yêu cầu

| Thành phần | Phiên bản | Ghi chú |
|-----------|-----------|---------|
| Java | 17+ | `winget install Microsoft.OpenJDK.17` |
| Maven | 3.6+ | `winget install Apache.Maven` |
| Chrome/Firefox/Edge | Bất kỳ | Cho UI tests (Selenium tự tải driver) |
| FastAPI Backend | Running | Port 5001 - cho API tests |
| Flask Frontend | Running | Port 5000 - cho UI tests |

## Build & Chạy

### Windows
```bat
build.bat      # Build JAR
run.bat        # Chạy app
```

### macOS / Linux
```bash
chmod +x build_and_run.sh && ./build_and_run.sh
```

---

## 2 Chế độ Test

### 1. API Tests (Backend)
- Gọi trực tiếp FastAPI REST endpoints
- Không cần browser
- Nhanh (~1-2 phút cho tất cả modules)
- Server cần: `http://localhost:5001`

### 2. UI Tests (Selenium WebDriver)
- Mở Chrome/Firefox/Edge thật sự
- Điều khiển giao diện như người dùng thật
- Chậm hơn (~5-10 phút)
- Servers cần: `http://localhost:5000` + `http://localhost:5001`
- **WebDriverManager tự động tải ChromeDriver** - không cần cài thủ công

---

## Modules được test

### API Tests (85+ test cases)

| Module | Tests | Nội dung |
|--------|-------|---------|
| Authentication | 7 | Login, logout, JWT, token blacklist |
| Products | 11 | CRUD sản phẩm, cache invalidation |
| Orders | 7 | Tạo/cập nhật đơn hàng, kiểm tra tồn kho |
| Invoices | 7 | Tạo hóa đơn, trừ tồn kho, ghi sổ kế toán |
| Discount Codes | 8 | Tạo/validate mã giảm giá |
| Shipments | 10 | Luồng trạng thái 8 bước, mã tracking VD... |
| Users & Permissions | 11 | CRUD nhân viên, RBAC, admin bypass |
| Reports & Analytics | 11 | Báo cáo doanh thu, công nợ, audit logs |
| Customers & Warehouse | 13 | Khách hàng CRM, kho hàng, khu vực |

### UI Tests (64+ test cases - Selenium)

| Module | Tests | Nội dung |
|--------|-------|---------|
| Login Page | 7 | Form validation, redirect, logout, auth guard |
| POS Interface | 11 | Tìm sản phẩm, thêm giỏ hàng, thanh toán |
| Products Page | 9 | Bảng sản phẩm, modal thêm/sửa/xóa |
| Orders & Invoices | 13 | Tab trạng thái, search, modal tạo mới |
| Shipping Page | 10 | 8 trạng thái card, modal tạo/cập nhật |
| Customers & Discounts | 14 | CRM khách hàng, mã giảm giá |

---

## Giao diện ứng dụng

```
┌─────────────────────────────────────────────────────────────┐
│  POS System Test Tool v2.0 — API + UI Selenium              │
├──────────────┬──────────────┬──────────────┬────────────────┤
│ ⚙ Config     │ ☑ Selection  │ ▶ Run Tests  │ 📊 Results     │
├──────────────┴──────────────┴──────────────┴────────────────┤
│  [Config Tab]                                               │
│  ┌── API Tests ──────────┐  ┌── UI Tests (Selenium) ──┐   │
│  │ Base URL              │  │ Frontend URL             │   │
│  │ Username / Password   │  │ Browser: [Chrome ▼]      │   │
│  │ Timeout, Retries      │  │ ☐ Headless mode          │   │
│  └───────────────────────┘  │ ☑ Screenshot on fail     │   │
│                             └──────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│  [Selection Tab]                                            │
│  ┌── API Tests ──────────┐ │ ┌── UI Tests (Selenium) ──┐  │
│  │ ☑ Authentication  7   │ │ │ ☐ Login Page         7  │  │
│  │ ☑ Products       11   │ │ │ ☐ POS Interface     11  │  │
│  │ ☑ Orders          7   │ │ │ ☐ Products Page      9  │  │
│  │ [All] [None] [Crit]   │ │ │ [All] [None]            │  │
│  └───────────────────────┘ │ └──────────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│  [Run Tab]                                                  │
│  [▶ Run API Tests] [◉ Run UI Tests] [⚡ Run All] [■ Cancel] │
│  Progress: ████████████░░░░ 75%                             │
│  ┌── Execution Log (dark terminal) ───────────────────────┐ │
│  │ ✓ [PASS] Authentication::Login - token received (120ms)│ │
│  │ ✓ [PASS] [UI] POS Interface::Add to cart (850ms)      │ │
│  │ ✗ [FAIL] Orders::Invalid product - expected 404 (45ms)│ │
│  └───────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  [Results Tab]                                              │
│  Total: 149 │ ✓ Passed: 131 │ ✗ Failed: 8 │ ⊘ Skipped: 10 │
│  [Export Results] [Copy Clipboard] [Clear]                  │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Time  │ Status │ Module        │ Test           │ ms   │ │
│  │ 10:05 │ PASS   │ [UI] POS     │ Add to cart    │ 850  │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Cấu trúc dự án

```
POSTestApp/
├── pom.xml
├── build.bat / run.bat / build_and_run.sh
└── src/main/java/com/pos/tester/
    ├── Main.java
    ├── api/
    │   ├── ApiClient.java        HTTP client (GET/POST/PUT/DELETE + JWT auth)
    │   └── TestContext.java      Shared state giữa các API test
    ├── model/
    │   ├── TestConfig.java       Cấu hình (backend URL, frontend URL, browser...)
    │   └── TestResult.java       Model kết quả test (PASS/FAIL/SKIP)
    ├── tests/                    API test suites (9 suites, 85+ tests)
    │   ├── BaseTestSuite.java
    │   ├── AuthTestSuite.java
    │   ├── ProductTestSuite.java
    │   ├── OrderTestSuite.java
    │   ├── InvoiceTestSuite.java
    │   ├── DiscountTestSuite.java
    │   ├── ShipmentTestSuite.java
    │   ├── UserPermissionTestSuite.java
    │   ├── ReportTestSuite.java
    │   ├── CustomerWarehouseTestSuite.java
    │   └── TestRunner.java
    ├── uitests/                  Selenium UI test suites (6 suites, 64+ tests)
    │   ├── BaseUITestSuite.java  Selenium helpers (wait, click, screenshot...)
    │   ├── LoginUITest.java      Login page automation
    │   ├── POSUITest.java        POS interface automation
    │   ├── ProductsUITest.java   Products page automation
    │   ├── OrdersInvoicesUITest.java
    │   ├── ShippingUITest.java
    │   ├── CustomersDiscountsUITest.java
    │   └── UITestRunner.java     Browser lifecycle + test orchestration
    └── ui/                       Java Swing GUI
        ├── MainFrame.java
        ├── ConfigPanel.java      Backend + Selenium settings (2 tabs)
        ├── TestSelectionPanel.java  API + UI module selection (split view)
        ├── RunPanel.java         3 run buttons (API / UI / All)
        └── ResultsPanel.java     Sortable table + Export/Copy
```
