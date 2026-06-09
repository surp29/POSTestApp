# POS System Test Tool v2.0

Ứng dụng desktop Java để test **API backend** và **UI frontend** của hệ thống POS.  
Tổng cộng **213 test cases** — 107 API + 106 UI Selenium.

---

## Yêu cầu

| Thành phần | Phiên bản | Ghi chú |
|-----------|-----------|---------|
| Java | 17+ | `winget install Microsoft.OpenJDK.17` |
| Maven | 3.6+ | `winget install Apache.Maven` |
| Microsoft Edge | 149 | `msedgedriver.exe` đi kèm sẵn trong thư mục gốc |
| Opera One | 132 | Hoặc dùng Edge — chọn trong Configuration |
| FastAPI Backend | Running | Port 5001 — cho API tests |
| Flask Frontend | Running | Port 5000 — cho UI tests |

> **Lưu ý browser:** Ứng dụng hỗ trợ **Edge 149** và **Opera One 132**.  
> `msedgedriver.exe` đã có sẵn trong thư mục gốc — không cần tải thêm.  
> Có thể chỉ định đường dẫn `.exe` tuỳ chỉnh trong tab Configuration → UI Tests.

---

## Build & Chạy

### Windows
```bat
build.bat      ← Build JAR (target/POSTestApp-1.0.0.jar)
run.bat        ← Chạy ứng dụng desktop
```

### Linux / macOS
```bash
chmod +x build_and_run.sh && ./build_and_run.sh
```

---

## 2 Chế độ Test

### 1. API Tests (Backend)
- Gọi trực tiếp FastAPI REST endpoints qua HTTP
- Không cần mở browser
- Nhanh (~1–2 phút cho tất cả 107 tests)
- Server cần: `http://localhost:5001`

### 2. UI Tests (Selenium WebDriver)
- Mở Edge hoặc Opera thật sự, điều khiển giao diện như người dùng
- Hỗ trợ **headless mode** (chạy không hiển thị cửa sổ)
- Chậm hơn (~8–12 phút cho 106 tests)
- Servers cần: `http://localhost:5000` + `http://localhost:5001`

---

## Modules được test

### API Tests — 107 test cases (9 modules)

| Module | Tests | Nội dung |
|--------|------:|---------|
| Authentication | 8 | Login, logout, JWT, refresh, token blacklist |
| Products | 11 | CRUD sản phẩm, cache Redis invalidation |
| Orders | 9 | Tạo/cập nhật/xóa đơn hàng, kiểm tra tồn kho |
| Invoices | 8 | Tạo hóa đơn từ POS, trừ tồn kho, ghi nhật ký |
| Discount Codes | 9 | Tạo/validate/xóa mã giảm giá (% và cố định) |
| Shipments | 10 | Luồng 8 trạng thái, mã tracking `VD{yymmdd}{6digits}` |
| Users & Permissions | 11 | CRUD nhân viên, RBAC, phân quyền module.action |
| Reports & Analytics | 20 | Doanh thu, công nợ, xếp hạng, audit log, chatbot AI, health |
| Customers & Warehouse | 21 | Khách hàng CRM, kho hàng, bảng giá, lịch làm việc |

### UI Tests — 106 test cases (8 modules, Selenium)

| Module | Tests | Nội dung |
|--------|------:|---------|
| Login Page | 8 | Form validation, redirect, đăng xuất, auth guard |
| POS Interface | 12 | Tìm sản phẩm, thêm giỏ hàng, modal thanh toán |
| Products Page | 10 | Bảng sản phẩm, search, modal thêm/sửa/xóa |
| Orders & Invoices | 14 | Tab trạng thái, tìm kiếm, modal tạo đơn/hóa đơn |
| Shipping Page | 11 | 8 stat-card trạng thái, modal tạo/cập nhật vận chuyển |
| Customers & Discounts | 15 | CRM khách hàng, CRUD mã giảm giá |
| Employees & Schedules | 12 | Danh sách nhân viên, phân quyền, lịch ca làm việc |
| Admin Pages | 24 | Kho hàng, bảng giá, nhóm SP, nhật ký, báo cáo, khu vực, cửa hàng, phân quyền |

---

## Giao diện ứng dụng

```
┌─────────────────────────────────────────────────────────────────┐
│  POS System Test Tool v2.0 — API + UI Selenium                  │
├──────────────┬──────────────┬───────────────┬───────────────────┤
│ ⚙ Config     │ ☑ Selection  │ ▶ Run Tests   │ 📊 Results        │
├──────────────┴──────────────┴───────────────┴───────────────────┤
│  [Config Tab]                                                   │
│  ┌── API Tests ───────────────┐  ┌── UI Tests (Selenium) ───┐  │
│  │ Base URL (port 5001)       │  │ Frontend URL (port 5000)  │  │
│  │ Username / Password        │  │ Browser: [Edge ▼]         │  │
│  │ Timeout (s), Retries       │  │ Custom binary path        │  │
│  │ Test Data Prefix           │  │ ☐ Headless mode           │  │
│  └────────────────────────────┘  │ ☑ Screenshot on fail      │  │
│                                  └───────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│  [Selection Tab]                                                │
│  ┌── API Tests ─────────────────┐ ┌── UI Tests ─────────────┐  │
│  │ ☑ Authentication          8  │ │ ☐ Login Page          8  │  │
│  │ ☑ Products               11  │ │ ☐ POS Interface      12  │  │
│  │ ☑ Reports & Analytics    20  │ │ ☐ Employees & Sched  12  │  │
│  │ ☑ Customers & Warehouse  21  │ │ ☐ Admin Pages        24  │  │
│  │ [All] [None] [Critical]      │ │ [All] [None]             │  │
│  └──────────────────────────────┘ └──────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│  [Run Tab]                                                      │
│  [▶ Run API Tests] [◉ Run UI Tests] [⚡ Run All] [■ Cancel]     │
│  Progress: ████████████░░░░ 75%                                 │
│  ┌── Execution Log ──────────────────────────────────────────┐  │
│  │ ✓ [PASS] Authentication::Login thành công (120ms)         │  │
│  │ ✓ [PASS] [UI] POS Interface::Thêm vào giỏ hàng (850ms)  │  │
│  │ ✗ [FAIL] Orders::Sản phẩm không tồn tại — expected 404   │  │
│  └───────────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│  [Results Tab]                                                  │
│  Total: 213 │ ✓ Passed: 207 │ ✗ Failed: 2 │ ⊘ Skipped: 4      │
│  [Export CSV] [Copy All] [Copy Failed] [Copy Passed] [Clear]    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Time  │ Status │ Module           │ Test          │  ms  │   │
│  │ 10:05 │ PASS   │ [UI] POS         │ Thêm giỏ hàng│  850 │   │
│  │ 10:07 │ FAIL   │ Products         │ Tạo sản phẩm │  340 │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

**Results panel có 4 nút copy:**
- **Copy All** — Sao chép toàn bộ kết quả
- **Copy Failed** — Chỉ sao chép các dòng FAIL (tiện báo cáo bug)
- **Copy Passed** — Chỉ sao chép các dòng PASS
- **Export CSV** — Xuất file `.csv` đầy đủ

---

## Cấu trúc dự án

```
POSTestApp/
├── build.bat               ← Build JAR (Windows)
├── run.bat                 ← Chạy app (Windows)
├── build_and_run.sh        ← Build + chạy (Linux/Mac)
├── mvnw.cmd                ← Maven wrapper
├── msedgedriver.exe        ← EdgeDriver 149 đi kèm sẵn
├── pom.xml
└── src/main/java/com/pos/tester/
    ├── Main.java
    ├── api/
    │   ├── ApiClient.java          HTTP client (GET/POST/PUT/DELETE + JWT)
    │   └── TestContext.java        Shared state giữa các API test
    ├── model/
    │   ├── TestConfig.java         Cấu hình (URL, browser, headless...)
    │   └── TestResult.java         Kết quả test (PASS/FAIL/SKIP + thời gian)
    ├── tests/                      API test suites — 107 tests
    │   ├── BaseTestSuite.java
    │   ├── AuthTestSuite.java           (8)  Login, JWT, blacklist
    │   ├── ProductTestSuite.java        (11) CRUD, cache
    │   ├── OrderTestSuite.java          (9)  Đơn hàng, tồn kho
    │   ├── InvoiceTestSuite.java        (8)  Hóa đơn, nhật ký
    │   ├── DiscountTestSuite.java       (9)  Mã giảm giá
    │   ├── ShipmentTestSuite.java       (10) 8-bước vận chuyển
    │   ├── UserPermissionTestSuite.java (11) RBAC, quyền nhân viên
    │   ├── ReportTestSuite.java         (20) Báo cáo, chatbot AI, health
    │   ├── CustomerWarehouseTestSuite.java (21) Khách hàng, kho, giá, lịch
    │   └── TestRunner.java
    ├── uitests/                    Selenium UI test suites — 106 tests
    │   ├── BaseUITestSuite.java         Selenium helpers (wait, jsClick, screenshot)
    │   ├── LoginUITest.java             (8)  Đăng nhập
    │   ├── POSUITest.java               (12) Giao diện POS
    │   ├── ProductsUITest.java          (10) Trang sản phẩm
    │   ├── OrdersInvoicesUITest.java    (14) Đơn hàng & hóa đơn
    │   ├── ShippingUITest.java          (11) Vận chuyển
    │   ├── CustomersDiscountsUITest.java (15) Khách hàng & mã giảm giá
    │   ├── EmployeesSchedulesUITest.java (12) Nhân viên & lịch làm việc
    │   ├── AdminPagesUITest.java        (24) Kho, bảng giá, báo cáo, khu vực...
    │   └── UITestRunner.java            Browser lifecycle, đăng nhập, điều phối
    └── ui/                         Java Swing GUI
        ├── MainFrame.java
        ├── ConfigPanel.java             Cấu hình API + Selenium (2 tabs)
        ├── TestSelectionPanel.java      Chọn module API / UI (split view)
        ├── RunPanel.java                3 nút chạy + log terminal
        └── ResultsPanel.java            Bảng kết quả, sort, Export, Copy
```

---

## Luồng hoạt động UI Tests

```
UITestRunner.runSelected()
    │
    ├─ initDriver()          ← Khởi động Edge/Opera với driver tương ứng
    ├─ driver.get("/login")  ← Mở trang đăng nhập
    ├─ LoginUITest.doLogin() ← Đăng nhập bằng tài khoản admin
    │
    └─ for each selected suite:
        ├─ navigateTo(path)  ← 1200ms pause sau load (dễ quan sát)
        ├─ jsClick(element)  ← 1000ms pause sau click (dễ quan sát)
        ├─ assertTrue / waitForVisibleSafe / waitForInvisibleSafe
        └─ TestResult → PASS / FAIL / SKIP
```

> Mỗi `navigateTo()` tự động chờ `document.readyState == complete` + 1200ms.  
> Mỗi `jsClick()` dùng JavaScript để click (vượt qua overlay) + chờ 1000ms.

---

## Cấu hình mặc định

| Tham số | Giá trị mặc định |
|---------|-----------------|
| Backend URL | `http://localhost:5001` |
| Frontend URL | `http://localhost:5000` |
| Admin username | `admin` |
| Admin password | `admin123` |
| Browser | Edge |
| Wait timeout (UI) | 5 giây |
| Headless | Tắt |
| Test data prefix | `TEST_` |
