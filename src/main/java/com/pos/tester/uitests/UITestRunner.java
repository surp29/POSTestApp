package com.pos.tester.uitests;

import com.pos.tester.model.TestConfig;
import com.pos.tester.model.TestResult;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

public class UITestRunner {
    private final TestConfig config;
    private WebDriver driver;
    private final List<TestResult> allResults = new ArrayList<>();
    private Consumer<TestResult> onTestComplete;
    private Consumer<String>     onStatusUpdate;
    private Consumer<Integer>    onProgressUpdate;
    private volatile boolean cancelled = false;

    // ── Known Windows binary paths ────────────────────────────────────────────
    private static final String USER_HOME = System.getProperty("user.home");
    private static final String LOCAL_APP = System.getenv("LOCALAPPDATA") != null
                                            ? System.getenv("LOCALAPPDATA")
                                            : USER_HOME + "\\AppData\\Local";

    private static final String[] EDGE_PATHS = {
        "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
        "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
        LOCAL_APP + "\\Microsoft\\Edge\\Application\\msedge.exe",
        USER_HOME + "\\AppData\\Local\\Microsoft\\Edge\\Application\\msedge.exe"
    };

    // Opera One 132 — typical install locations on Windows
    private static final String[] OPERA_PATHS = {
        LOCAL_APP + "\\Programs\\Opera One\\opera.exe",
        LOCAL_APP + "\\Programs\\Opera\\opera.exe",
        USER_HOME + "\\AppData\\Local\\Programs\\Opera One\\opera.exe",
        USER_HOME + "\\AppData\\Local\\Programs\\Opera\\opera.exe",
        "C:\\Program Files\\Opera One\\opera.exe",
        "C:\\Program Files\\Opera\\opera.exe",
        "C:\\Program Files (x86)\\Opera One\\opera.exe",
        "C:\\Program Files (x86)\\Opera\\opera.exe"
    };

    public UITestRunner(TestConfig config) { this.config = config; }

    public void setOnTestComplete(Consumer<TestResult> cb)  { this.onTestComplete   = cb; }
    public void setOnStatusUpdate(Consumer<String>     cb)  { this.onStatusUpdate   = cb; }
    public void setOnProgressUpdate(Consumer<Integer>  cb)  { this.onProgressUpdate = cb; }
    public void cancel() { this.cancelled = true; }

    // ─── Pre-flight check ─────────────────────────────────────────────────────
    /**
     * Returns null        → browser is ready.
     * Returns "warn:..."  → auto-switched to another browser.
     * Returns other string → fatal: no browser found.
     */
    public String checkBrowserAvailable() {
        String custom = config.getCustomBinaryPath();
        if (custom != null && !custom.isBlank()) {
            return new File(custom).exists() ? null
                    : "Custom binary not found:\n" + custom;
        }
        if (findBinary(config.getBrowserType()) != null) return null;

        // Auto-fallback across all available types
        for (TestConfig.BrowserType fb : TestConfig.BrowserType.values()) {
            if (fb == config.getBrowserType()) continue;
            if (findBinary(fb) != null) {
                String prev = config.getBrowserType().toString();
                config.setBrowserType(fb);
                return "warn:" + prev + " not found — auto-switched to " + fb + ".";
            }
        }
        return "No supported browser found on this machine.\n\n"
             + "Expected locations checked:\n"
             + "  Edge  → C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe\n"
             + "  Opera → " + LOCAL_APP + "\\Programs\\Opera One\\opera.exe\n\n"
             + "Or set a custom binary path in Configuration → UI Tests tab.";
    }

    /** Returns map: BrowserType → binary path for each installed browser. */
    public static Map<TestConfig.BrowserType, String> detectInstalledBrowsers() {
        Map<TestConfig.BrowserType, String> found = new LinkedHashMap<>();
        tryAdd(TestConfig.BrowserType.EDGE,  EDGE_PATHS,  found);
        tryAdd(TestConfig.BrowserType.OPERA, OPERA_PATHS, found);
        return found;
    }

    // ─── Main entry point ─────────────────────────────────────────────────────

    public List<TestResult> runSelected(Set<String> selectedModules) throws Exception {
        cancelled = false;
        allResults.clear();

        updateStatus("Launching " + config.getBrowserType() + "...");
        driver = initDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
        driver.manage().window().maximize();

        try {
            updateStatus("Opening " + config.getFrontendUrl() + "/login ...");
            try {
                driver.get(config.getFrontendUrl() + "/login");
                Thread.sleep(1500);
            } catch (Exception e) {
                throw new Exception(
                    "Cannot open Flask frontend at " + config.getFrontendUrl() + "\n"
                  + "Make sure Flask server is running (python app.py).\n"
                  + "Detail: " + e.getMessage());
            }

            LoginUITest loginHelper = makeLoginSuite();

            if (selectedModules.contains("Login Page")) {
                updateStatus("Running: Login Page tests...");
                wire(loginHelper);
                loginHelper.runAll();
            }

            updateStatus("Logging in as " + config.getAdminUsername() + "...");
            loginHelper.doLogin();
            Thread.sleep(800);

            String url = driver.getCurrentUrl();
            if (url != null && url.contains("/login")) {
                throw new Exception(
                    "Login failed – still on /login page.\n"
                  + "Check credentials in Configuration:\n"
                  + "  Username : " + config.getAdminUsername() + "\n"
                  + "  Frontend : " + config.getFrontendUrl());
            }
            updateStatus("Login OK. Running UI tests...");

            List<BaseUITestSuite> suites = buildSuites();
            List<BaseUITestSuite> toRun  = suites.stream()
                    .filter(s -> selectedModules.contains(s.getModuleName()))
                    .toList();

            int total = toRun.size(), done = 0;
            for (BaseUITestSuite suite : toRun) {
                if (cancelled) break;
                updateStatus("[UI] Running: " + suite.getModuleName() + "...");
                wire(suite);
                suite.runAll();
                done++;
                if (onProgressUpdate != null)
                    onProgressUpdate.accept((int)((double) done / Math.max(total, 1) * 100));
            }

        } finally {
            updateStatus("Closing browser...");
            if (driver != null) {
                try { driver.quit(); } catch (Exception ignored) {}
                driver = null;
            }
        }

        updateStatus("UI tests complete: " + passCount() + " passed, "
                + failCount() + " failed, " + skipCount() + " skipped.");
        return allResults;
    }

    // ─── Browser initialisation ───────────────────────────────────────────────

    private WebDriver initDriver() throws Exception {
        boolean headless = config.isHeadless();
        String  custom   = config.getCustomBinaryPath();

        switch (config.getBrowserType()) {

            case EDGE -> {
                // Edge ships msedgedriver.exe in its versioned subfolder — use it directly,
                // no internet required.
                String edgeDriver = findLocalEdgeDriver();
                if (edgeDriver != null) {
                    updateStatus("Using bundled EdgeDriver: " + edgeDriver);
                    System.setProperty("webdriver.edge.driver", edgeDriver);
                } else {
                    updateStatus("Local EdgeDriver not found — trying download...");
                    try {
                        WebDriverManager.edgedriver().browserVersion("149").setup();
                    } catch (Exception wdmEx) {
                        throw new Exception(
                            "Cannot start EdgeDriver.\n\n"
                          + "msedgedriver.exe was not found in the Edge installation folder,\n"
                          + "and the automatic download failed (no internet?).\n\n"
                          + "Fix: make sure Edge 149 is properly installed at\n"
                          + "  C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\\n"
                          + "Detail: " + wdmEx.getMessage());
                    }
                }
                EdgeOptions opts = new EdgeOptions();
                String bin = resolveBin(custom, TestConfig.BrowserType.EDGE);
                if (bin != null) { opts.setBinary(bin); updateStatus("Edge binary: " + bin); }
                if (headless) opts.addArguments("--headless=new", "--window-size=1920,1080");
                opts.addArguments("--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage",
                        "--disable-extensions", "--disable-notifications",
                        "--disable-popup-blocking", "--lang=vi-VN", "--start-maximized");
                return new EdgeDriver(opts);
            }

            case OPERA -> {
                // Opera One is Chromium-based → use ChromeDriver with Opera binary.
                // Try WebDriverManager cache first; fall back if no network.
                String bin = resolveBin(custom, TestConfig.BrowserType.OPERA);
                if (bin == null) {
                    throw new Exception(
                        "Opera One binary not found.\n"
                      + "Expected: " + LOCAL_APP + "\\Programs\\Opera One\\opera.exe\n"
                      + "Or set a custom path in Configuration → UI Tests → Custom .exe");
                }
                updateStatus("Opera binary: " + bin);
                // Try to find a local operadriver/chromedriver first
                String operaDriver = findLocalOperaDriver(bin);
                if (operaDriver != null) {
                    updateStatus("Using local OperaDriver: " + operaDriver);
                    System.setProperty("webdriver.chrome.driver", operaDriver);
                } else {
                    updateStatus("Downloading OperaDriver for Opera 132...");
                    try {
                        WebDriverManager.operadriver().browserVersion("132").setup();
                    } catch (Exception wdmEx) {
                        throw new Exception(
                            "Cannot download OperaDriver (no internet connection?).\n"
                          + "Detail: " + wdmEx.getMessage());
                    }
                }
                ChromeOptions opts = new ChromeOptions();
                opts.setBinary(bin);
                if (headless) opts.addArguments("--headless=new", "--window-size=1920,1080");
                opts.addArguments("--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage",
                        "--disable-extensions", "--disable-notifications",
                        "--disable-popup-blocking", "--lang=vi-VN", "--start-maximized");
                return new ChromeDriver(opts);
            }

            default -> throw new Exception("Unsupported browser: " + config.getBrowserType());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Searches for msedgedriver.exe in order:
     *  1. Same directory as the running JAR (user placed it there manually)
     *  2. Current working directory
     *  3. Edge versioned subfolder, e.g. Application\149.0.4022.52\msedgedriver.exe
     */
    private String findLocalEdgeDriver() {
        // 1. Next to the running JAR
        String jarDir = getJarDir();
        if (jarDir != null) {
            File f = new File(jarDir, "msedgedriver.exe");
            if (f.exists()) { return f.getAbsolutePath(); }
        }
        // 2. Current working directory
        File cwd = new File(System.getProperty("user.dir"), "msedgedriver.exe");
        if (cwd.exists()) return cwd.getAbsolutePath();

        // 3. Edge installation versioned subfolder
        for (String edgePath : EDGE_PATHS) {
            File edge = new File(edgePath);
            if (!edge.exists()) continue;
            File appDir = edge.getParentFile();
            if (appDir == null || !appDir.isDirectory()) continue;
            File[] versionDirs = appDir.listFiles(f ->
                f.isDirectory() && f.getName().matches("\\d+\\.\\d+\\.\\d+\\.\\d+"));
            if (versionDirs == null) continue;
            for (File vd : versionDirs) {
                File driver = new File(vd, "msedgedriver.exe");
                if (driver.exists()) return driver.getAbsolutePath();
            }
        }
        return null;
    }

    /** Returns the directory containing the running JAR, or null if running from IDE. */
    private String getJarDir() {
        try {
            File jar = new File(
                UITestRunner.class.getProtectionDomain()
                                  .getCodeSource()
                                  .getLocation()
                                  .toURI());
            File dir = jar.isFile() ? jar.getParentFile() : jar;
            return dir != null ? dir.getAbsolutePath() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Opera may ship operadriver.exe alongside the opera.exe binary.
     */
    private String findLocalOperaDriver(String operaBinPath) {
        File operaBin = new File(operaBinPath);
        if (!operaBin.exists()) return null;
        File dir = operaBin.getParentFile();
        if (dir == null) return null;
        // Check for operadriver.exe or chromedriver.exe next to opera.exe
        for (String name : new String[]{"operadriver.exe", "chromedriver.exe"}) {
            File d = new File(dir, name);
            if (d.exists()) return d.getAbsolutePath();
        }
        return null;
    }

    private String resolveBin(String custom, TestConfig.BrowserType type) {
        if (custom != null && !custom.isBlank() && new File(custom).exists()) return custom;
        return findBinary(type);
    }

    private String findBinary(TestConfig.BrowserType type) {
        String[] paths = switch (type) {
            case EDGE  -> EDGE_PATHS;
            case OPERA -> OPERA_PATHS;
        };
        for (String p : paths) {
            if (p != null && !p.isBlank() && new File(p).exists()) return p;
        }
        return null;
    }

    private static void tryAdd(TestConfig.BrowserType type, String[] paths,
                               Map<TestConfig.BrowserType, String> result) {
        for (String p : paths) {
            if (p != null && !p.isBlank() && new File(p).exists()) {
                result.put(type, p);
                return;
            }
        }
    }

    private LoginUITest makeLoginSuite() {
        return new LoginUITest(driver,
                config.getFrontendUrl(), config.getScreenshotDir(),
                config.getUiWaitSeconds(),
                config.getAdminUsername(), config.getAdminPassword());
    }

    private void wire(BaseUITestSuite suite) {
        suite.setOnTestComplete(r -> {
            allResults.add(r);
            if (onTestComplete != null) onTestComplete.accept(r);
        });
    }

    private List<BaseUITestSuite> buildSuites() {
        String sd = config.getScreenshotDir();
        String fu = config.getFrontendUrl();
        int    w  = config.getUiWaitSeconds();
        String px = config.getTestDataPrefix();
        return List.of(
                new POSUITest(driver, fu, sd, w),
                new ProductsUITest(driver, fu, sd, w, px),
                new OrdersInvoicesUITest(driver, fu, sd, w),
                new ShippingUITest(driver, fu, sd, w),
                new CustomersDiscountsUITest(driver, fu, sd, w, px),
                new EmployeesSchedulesUITest(driver, fu, sd, w),
                new AdminPagesUITest(driver, fu, sd, w)
        );
    }

    // ─── Module catalog ───────────────────────────────────────────────────────

    public Map<String, String> getModuleDescriptions() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Login Page",            "UI: Login form, validation, redirect, logout");
        m.put("POS Interface",         "UI: Product search, Add to cart, Payment modal");
        m.put("Products Page",         "UI: Product list, Search, Create/Edit/Delete");
        m.put("Orders & Invoices",     "UI: Status tabs, Search, Create order/invoice");
        m.put("Shipping Page",         "UI: 8 status cards, Create/Update shipment");
        m.put("Customers & Discounts", "UI: Customer CRM, Discount codes CRUD");
        m.put("Employees & Schedules", "UI: Employee list, stats, add/edit, Schedule calendar");
        m.put("Admin Pages",           "UI: Warehouse, Prices, ProductGroups, Reports, Diary, Areas, Shops, Permissions");
        return m;
    }

    public long passCount() { return allResults.stream().filter(r -> r.getStatus() == TestResult.Status.PASS).count(); }
    public long failCount() { return allResults.stream().filter(r -> r.getStatus() == TestResult.Status.FAIL).count(); }
    public long skipCount() { return allResults.stream().filter(r -> r.getStatus() == TestResult.Status.SKIP).count(); }

    private void updateStatus(String msg) { if (onStatusUpdate != null) onStatusUpdate.accept(msg); }
}
