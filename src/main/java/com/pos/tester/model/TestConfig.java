package com.pos.tester.model;

public class TestConfig {
    // Backend (FastAPI)
    private String baseUrl;
    private String adminUsername;
    private String adminPassword;
    private int timeoutSeconds;
    private boolean stopOnFirstFailure;
    private boolean verboseLogging;
    private String testDataPrefix;
    private int connectionRetries;

    // Frontend (Flask) + Selenium
    private String frontendUrl;
    private BrowserType browserType;
    private boolean headless;
    private boolean screenshotOnFail;
    private String screenshotDir;
    private int uiWaitSeconds;
    private String customBinaryPath;

    public enum BrowserType {
        EDGE("Microsoft Edge"),
        OPERA("Opera One");

        private final String label;
        BrowserType(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public TestConfig() {
        this.baseUrl            = "http://localhost:5001";
        this.adminUsername      = "admin";
        this.adminPassword      = "admin123";
        this.timeoutSeconds     = 30;
        this.stopOnFirstFailure = false;
        this.verboseLogging     = true;
        this.testDataPrefix     = "TEST_";
        this.connectionRetries  = 3;

        this.frontendUrl        = "http://localhost:5000";
        this.browserType        = BrowserType.EDGE;    // Edge is default (pre-installed on Windows 11)
        this.headless           = false;
        this.screenshotOnFail   = true;
        this.screenshotDir      = System.getProperty("user.home") + "\\POSTestScreenshots";
        this.uiWaitSeconds      = 12;
        this.customBinaryPath   = "";
    }

    // ── Backend ───────────────────────────────────────────────────────────────
    public String  getBaseUrl()             { return baseUrl; }
    public void    setBaseUrl(String v)     { this.baseUrl = v.trim().replaceAll("/$",""); }
    public String  getAdminUsername()       { return adminUsername; }
    public void    setAdminUsername(String v){ this.adminUsername = v; }
    public String  getAdminPassword()       { return adminPassword; }
    public void    setAdminPassword(String v){ this.adminPassword = v; }
    public int     getTimeoutSeconds()      { return timeoutSeconds; }
    public void    setTimeoutSeconds(int v) { this.timeoutSeconds = v; }
    public boolean isStopOnFirstFailure()   { return stopOnFirstFailure; }
    public void    setStopOnFirstFailure(boolean v){ this.stopOnFirstFailure = v; }
    public boolean isVerboseLogging()       { return verboseLogging; }
    public void    setVerboseLogging(boolean v){ this.verboseLogging = v; }
    public String  getTestDataPrefix()      { return testDataPrefix; }
    public void    setTestDataPrefix(String v){ this.testDataPrefix = v; }
    public int     getConnectionRetries()   { return connectionRetries; }
    public void    setConnectionRetries(int v){ this.connectionRetries = v; }

    // ── Frontend / Selenium ───────────────────────────────────────────────────
    public String  getFrontendUrl()         { return frontendUrl; }
    public void    setFrontendUrl(String v) { this.frontendUrl = v.trim().replaceAll("/$",""); }
    public BrowserType getBrowserType()     { return browserType; }
    public void    setBrowserType(BrowserType v){ this.browserType = v; }
    public boolean isHeadless()             { return headless; }
    public void    setHeadless(boolean v)   { this.headless = v; }
    public boolean isScreenshotOnFail()     { return screenshotOnFail; }
    public void    setScreenshotOnFail(boolean v){ this.screenshotOnFail = v; }
    public String  getScreenshotDir()       { return screenshotDir; }
    public void    setScreenshotDir(String v){ this.screenshotDir = v; }
    public int     getUiWaitSeconds()       { return uiWaitSeconds; }
    public void    setUiWaitSeconds(int v)  { this.uiWaitSeconds = v; }
    public String  getCustomBinaryPath()    { return customBinaryPath; }
    public void    setCustomBinaryPath(String v){ this.customBinaryPath = (v==null?"":v.trim()); }
}
