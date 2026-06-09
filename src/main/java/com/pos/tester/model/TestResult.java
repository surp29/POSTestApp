package com.pos.tester.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TestResult {
    public enum Status { PASS, FAIL, SKIP, RUNNING }

    private final String moduleName;
    private final String testName;
    private Status status;
    private String message;
    private String details;
    private long durationMs;
    private final LocalDateTime startTime;

    public TestResult(String moduleName, String testName) {
        this.moduleName = moduleName;
        this.testName = testName;
        this.status = Status.RUNNING;
        this.startTime = LocalDateTime.now();
        this.durationMs = 0;
    }

    public void pass(String message) {
        this.status = Status.PASS;
        this.message = message;
        this.durationMs = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
    }

    public void fail(String message) {
        this.status = Status.FAIL;
        this.message = message;
        this.durationMs = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
    }

    public void fail(String message, String details) {
        this.status = Status.FAIL;
        this.message = message;
        this.details = details;
        this.durationMs = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
    }

    public void skip(String reason) {
        this.status = Status.SKIP;
        this.message = reason;
    }

    public String getModuleName() { return moduleName; }
    public String getTestName() { return testName; }
    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public String getDetails() { return details; }
    public long getDurationMs() { return durationMs; }
    public LocalDateTime getStartTime() { return startTime; }

    public String getStatusIcon() {
        return switch (status) {
            case PASS -> "✓";
            case FAIL -> "✗";
            case SKIP -> "⊘";
            case RUNNING -> "⏳";
        };
    }

    public String getTimestamp() {
        return startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    @Override
    public String toString() {
        return String.format("[%s] %s::%s - %s (%dms)",
                getStatusIcon(), moduleName, testName, message, durationMs);
    }
}
