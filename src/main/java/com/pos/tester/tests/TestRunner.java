package com.pos.tester.tests;

import com.pos.tester.api.ApiClient;
import com.pos.tester.api.TestContext;
import com.pos.tester.model.TestConfig;
import com.pos.tester.model.TestResult;

import java.util.*;
import java.util.function.Consumer;

public class TestRunner {
    private final TestConfig config;
    private final List<BaseTestSuite> allSuites = new ArrayList<>();
    private final List<TestResult> allResults = new ArrayList<>();
    private Consumer<TestResult> onTestComplete;
    private Consumer<String> onStatusUpdate;
    private Consumer<Integer> onProgressUpdate;
    private volatile boolean cancelled = false;

    public TestRunner(TestConfig config) {
        this.config = config;
    }

    public void setOnTestComplete(Consumer<TestResult> callback) { this.onTestComplete = callback; }
    public void setOnStatusUpdate(Consumer<String> callback) { this.onStatusUpdate = callback; }
    public void setOnProgressUpdate(Consumer<Integer> callback) { this.onProgressUpdate = callback; }

    public void cancel() { this.cancelled = true; }

    public List<TestResult> runSelected(Set<String> selectedModules) throws Exception {
        cancelled = false;
        allResults.clear();
        ApiClient client = new ApiClient(config);

        updateStatus("Connecting to " + config.getBaseUrl() + "...");
        if (!client.isConnected()) {
            throw new Exception("Cannot connect to " + config.getBaseUrl() + ". Please check if the server is running.");
        }

        updateStatus("Authenticating as " + config.getAdminUsername() + "...");
        ApiClient.ApiResponse loginResp = client.login(config.getAdminUsername(), config.getAdminPassword());
        if (!loginResp.success) {
            throw new Exception("Authentication failed: " + loginResp.body);
        }
        updateStatus("Authenticated successfully. Running tests...");

        TestContext ctx = new TestContext(client);
        List<BaseTestSuite> suites = buildSuites(ctx);

        List<BaseTestSuite> toRun = suites.stream()
                .filter(s -> selectedModules.contains(s.getModuleName()))
                .toList();

        int totalSuites = toRun.size();
        int completedSuites = 0;

        for (BaseTestSuite suite : toRun) {
            if (cancelled) break;
            updateStatus("Running: " + suite.getModuleName() + "...");
            suite.setOnTestComplete(r -> {
                allResults.add(r);
                if (onTestComplete != null) onTestComplete.accept(r);
            });

            try {
                suite.runAll();
                if (config.isStopOnFirstFailure() && suite.failCount() > 0) {
                    updateStatus("Stopped after first failure in " + suite.getModuleName());
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            completedSuites++;
            int progress = (int) ((double) completedSuites / totalSuites * 100);
            if (onProgressUpdate != null) onProgressUpdate.accept(progress);
        }

        updateStatus(cancelled ? "Tests cancelled." :
                "Tests completed: " + passCount() + " passed, " + failCount() + " failed, " + skipCount() + " skipped.");
        return allResults;
    }

    private List<BaseTestSuite> buildSuites(TestContext ctx) {
        return List.of(
                new AuthTestSuite(ctx, config),
                new ProductTestSuite(ctx, config),
                new OrderTestSuite(ctx, config),
                new InvoiceTestSuite(ctx, config),
                new DiscountTestSuite(ctx, config),
                new ShipmentTestSuite(ctx, config),
                new UserPermissionTestSuite(ctx, config),
                new ReportTestSuite(ctx, config),
                new CustomerWarehouseTestSuite(ctx, config)
        );
    }

    public List<BaseTestSuite> getAllSuites(TestContext ctx) {
        return buildSuites(ctx);
    }

    public List<String> getAvailableModules() {
        TestContext dummy = new TestContext(new ApiClient(config));
        return buildSuites(dummy).stream().map(BaseTestSuite::getModuleName).toList();
    }

    public Map<String, String> getModuleDescriptions() {
        TestContext dummy = new TestContext(new ApiClient(config));
        Map<String, String> map = new LinkedHashMap<>();
        for (BaseTestSuite s : buildSuites(dummy)) {
            map.put(s.getModuleName(), s.getDescription());
        }
        return map;
    }

    public long passCount() { return allResults.stream().filter(r -> r.getStatus() == TestResult.Status.PASS).count(); }
    public long failCount() { return allResults.stream().filter(r -> r.getStatus() == TestResult.Status.FAIL).count(); }
    public long skipCount() { return allResults.stream().filter(r -> r.getStatus() == TestResult.Status.SKIP).count(); }

    private void updateStatus(String msg) {
        if (onStatusUpdate != null) onStatusUpdate.accept(msg);
    }
}
