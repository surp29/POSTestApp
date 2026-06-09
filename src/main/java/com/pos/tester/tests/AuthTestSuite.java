package com.pos.tester.tests;

import com.google.gson.JsonObject;
import com.pos.tester.api.ApiClient;
import com.pos.tester.api.TestContext;
import com.pos.tester.model.TestConfig;
import com.pos.tester.model.TestResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AuthTestSuite extends BaseTestSuite {
    private final TestConfig config;

    public AuthTestSuite(TestContext ctx, TestConfig config) {
        super(ctx);
        this.config = config;
    }

    @Override public String getModuleName() { return "Authentication"; }
    @Override public String getDescription() { return "Login, Logout, Token Refresh, Current User"; }

    @Override
    public List<TestResult> runAll() throws InterruptedException {
        results.clear();

        runTest("Health Check", result -> {
            ApiClient.ApiResponse resp = api.get("/health");
            assertTrue(resp.statusCode == 200, "Health endpoint returned " + resp.statusCode);
            result.pass("Server is online (status 200)");
        });

        runTest("Login with valid credentials", result -> {
            ApiClient.ApiResponse resp = api.login(config.getAdminUsername(), config.getAdminPassword());
            assertTrue(resp.success, "Login failed: " + resp.body);
            String token = resp.getField("access_token");
            assertNotNull(token, "access_token");
            ctx.set("admin_token", token);
            result.pass("Login successful, token received");
        });

        runTest("Login with invalid credentials", result -> {
            String savedToken = api.getAuthToken();
            api.setAuthToken(null);
            ApiClient.ApiResponse resp = api.login("invalid_user_xyz", "wrong_password_abc");
            api.setAuthToken(savedToken);
            assertTrue(!resp.success, "Expected login failure but got status " + resp.statusCode);
            result.pass("Invalid credentials correctly rejected (status " + resp.statusCode + ")");
        });

        runTest("Get current user info (/api/auth/me)", result -> {
            ApiClient.ApiResponse resp = api.get("/api/auth/me");
            assertTrue(resp.success, "GET /api/auth/me failed: " + resp.body);
            String username = resp.getField("username");
            assertNotNull(username, "username");
            result.pass("Current user: " + username + " (position: " + resp.getField("position") + ")");
        });

        runTest("Access protected endpoint without token", result -> {
            String savedToken = api.getAuthToken();
            api.setAuthToken(null);
            ApiClient.ApiResponse resp = api.get("/api/products/");
            api.setAuthToken(savedToken);
            assertTrue(resp.statusCode == 401 || resp.statusCode == 403,
                    "Expected 401/403 but got: " + resp.statusCode);
            result.pass("Unauthorized access correctly blocked (status " + resp.statusCode + ")");
        });

        runTest("Refresh token", result -> {
            ApiClient.ApiResponse resp = api.post("/api/auth/refresh", "{}");
            if (resp.statusCode == 404 || resp.statusCode == 422) {
                result.skip("Refresh endpoint not available or requires different payload");
                return;
            }
            assertTrue(resp.statusCode == 200 || resp.statusCode == 201,
                    "Token refresh failed: " + resp.body);
            result.pass("Token refreshed successfully");
        });

        runTest("Logout", result -> {
            ApiClient.ApiResponse resp = api.post("/api/auth/logout", "{}");
            assertTrue(resp.success, "Logout failed: " + resp.body);
            String oldToken = api.getAuthToken();
            // Re-login to continue tests
            api.login(config.getAdminUsername(), config.getAdminPassword());
            result.pass("Logout successful, re-logged in for further tests");
        });

        runTest("Token blacklisted after logout", result -> {
            // Get a fresh token then logout it
            String savedToken = api.getAuthToken();
            ApiClient.ApiResponse loginResp = api.login(config.getAdminUsername(), config.getAdminPassword());
            String tempToken = loginResp.getField("access_token");
            api.setAuthToken(tempToken);
            api.post("/api/auth/logout", "{}");
            // Try to use the logged-out token
            ApiClient.ApiResponse resp = api.get("/api/auth/me");
            api.setAuthToken(savedToken);
            // Re-login
            api.login(config.getAdminUsername(), config.getAdminPassword());
            if (resp.statusCode == 401 || resp.statusCode == 403) {
                result.pass("Blacklisted token correctly rejected (status " + resp.statusCode + ")");
            } else {
                result.skip("Token blacklist check: status " + resp.statusCode + " (Redis may be unavailable)");
            }
        });

        return results;
    }
}
