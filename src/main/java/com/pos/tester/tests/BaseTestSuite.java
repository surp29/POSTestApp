package com.pos.tester.tests;

import com.pos.tester.api.ApiClient;
import com.pos.tester.api.TestContext;
import com.pos.tester.model.TestResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class BaseTestSuite {
    protected final TestContext ctx;
    protected final ApiClient api;
    protected final List<TestResult> results = new ArrayList<>();
    protected Consumer<TestResult> onTestComplete;

    public BaseTestSuite(TestContext ctx) {
        this.ctx = ctx;
        this.api = ctx.getApiClient();
    }

    public void setOnTestComplete(Consumer<TestResult> callback) {
        this.onTestComplete = callback;
    }

    public abstract String getModuleName();
    public abstract String getDescription();
    public abstract List<TestResult> runAll() throws InterruptedException;

    protected TestResult runTest(String testName, TestAction action) {
        TestResult result = new TestResult(getModuleName(), testName);
        try {
            action.execute(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.fail("Test interrupted");
        } catch (Exception e) {
            result.fail("Exception: " + e.getMessage(),
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        results.add(result);
        if (onTestComplete != null) onTestComplete.accept(result);
        return result;
    }

    public List<TestResult> getResults() { return results; }

    public long passCount() { return results.stream().filter(r -> r.getStatus() == TestResult.Status.PASS).count(); }
    public long failCount() { return results.stream().filter(r -> r.getStatus() == TestResult.Status.FAIL).count(); }
    public long skipCount() { return results.stream().filter(r -> r.getStatus() == TestResult.Status.SKIP).count(); }

    @FunctionalInterface
    public interface TestAction {
        void execute(TestResult result) throws Exception;
    }

    protected void assertTrue(boolean condition, String failMessage) throws AssertionError {
        if (!condition) throw new AssertionError(failMessage);
    }

    protected void assertEquals(Object expected, Object actual, String fieldName) throws AssertionError {
        if (expected == null && actual == null) return;
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(fieldName + " expected=" + expected + " actual=" + actual);
        }
    }

    protected void assertNotNull(Object value, String fieldName) throws AssertionError {
        if (value == null) throw new AssertionError(fieldName + " must not be null");
    }

    protected void assertContains(String haystack, String needle) throws AssertionError {
        if (haystack == null || !haystack.contains(needle)) {
            throw new AssertionError("Expected response to contain '" + needle + "' but got: " + haystack);
        }
    }
}
