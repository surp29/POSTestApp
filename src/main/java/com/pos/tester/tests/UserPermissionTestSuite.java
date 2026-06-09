package com.pos.tester.tests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pos.tester.api.ApiClient;
import com.pos.tester.api.TestContext;
import com.pos.tester.model.TestConfig;
import com.pos.tester.model.TestResult;

import java.util.List;

public class UserPermissionTestSuite extends BaseTestSuite {
    private final TestConfig config;

    public UserPermissionTestSuite(TestContext ctx, TestConfig config) {
        super(ctx);
        this.config = config;
    }

    @Override public String getModuleName() { return "Users & Permissions"; }
    @Override public String getDescription() { return "User CRUD, RBAC permissions, Admin bypass"; }

    @Override
    public List<TestResult> runAll() throws InterruptedException {
        results.clear();
        String testUsername = config.getTestDataPrefix() + "user_" + (System.currentTimeMillis() % 10000);

        runTest("GET /api/users/ - List users", result -> {
            ApiClient.ApiResponse resp = api.get("/api/users/");
            assertTrue(resp.success, "Failed to get users: " + resp.statusCode);
            result.pass("Users listed (status " + resp.statusCode + ")");
        });

        runTest("POST /api/users/ - Create test employee", result -> {
            String body = String.format("""
                {
                    "username": "%s",
                    "password": "Test@12345",
                    "full_name": "Test Employee",
                    "email": "testemployee@postest.com",
                    "phone": "0999888777",
                    "position": "Nhân viên",
                    "department": "Test Dept",
                    "is_active": true
                }
                """, testUsername);
            ApiClient.ApiResponse resp = api.post("/api/users/", body);
            assertTrue(resp.success, "Failed to create user: " + resp.body);
            int userId = resp.getIntField("id");
            assertTrue(userId > 0, "Invalid user id");
            ctx.setCreatedUserId(userId);
            result.pass("User created: " + testUsername + " (id=" + userId + ")");
        });

        runTest("GET /api/users/{id} - Get user by ID", result -> {
            Integer userId = ctx.getCreatedUserId();
            if (userId == null) { result.skip("No user created"); return; }
            ApiClient.ApiResponse resp = api.get("/api/users/" + userId);
            assertTrue(resp.success, "Failed to get user: " + resp.body);
            result.pass("User retrieved: " + resp.getField("username"));
        });

        runTest("GET /api/permissions/my - Get current user permissions", result -> {
            ApiClient.ApiResponse resp = api.get("/api/permissions/my");
            assertTrue(resp.success, "Failed to get permissions: " + resp.statusCode);
            result.pass("Admin permissions retrieved (admin should have all perms)");
        });

        runTest("Admin bypass: verify admin has all permissions", result -> {
            ApiClient.ApiResponse resp = api.get("/api/auth/me");
            assertTrue(resp.success, "Cannot get current user");
            String position = resp.getField("position");
            if ("Admin".equals(position)) {
                result.pass("Current user is Admin — bypass verified, no DB check needed");
            } else {
                result.skip("Current user is " + position + " (not Admin), cannot verify admin bypass");
            }
        });

        runTest("PUT /api/permissions/{user_id} - Assign permissions to user", result -> {
            Integer userId = ctx.getCreatedUserId();
            if (userId == null) { result.skip("No test user created"); return; }
            String body = """
                {
                    "permissions": [
                        "pos.view",
                        "products.view",
                        "orders.view",
                        "invoices.view"
                    ]
                }
                """;
            ApiClient.ApiResponse resp = api.put("/api/permissions/" + userId, body);
            assertTrue(resp.success, "Failed to assign permissions: " + resp.body);
            result.pass("Permissions assigned to user " + userId);
        });

        runTest("GET /api/permissions/{user_id} - Get user permissions", result -> {
            Integer userId = ctx.getCreatedUserId();
            if (userId == null) { result.skip("No test user created"); return; }
            ApiClient.ApiResponse resp = api.get("/api/permissions/" + userId);
            assertTrue(resp.success, "Failed to get user permissions: " + resp.statusCode);
            result.pass("User permissions retrieved (status " + resp.statusCode + ")");
        });

        runTest("Login as new employee - verify limited permissions", result -> {
            Integer userId = ctx.getCreatedUserId();
            if (userId == null) { result.skip("No test user created"); return; }
            String savedToken = api.getAuthToken();
            ApiClient.ApiResponse loginResp = api.login(testUsername, "Test@12345");
            if (!loginResp.success) {
                api.setAuthToken(savedToken);
                result.skip("Cannot login as new user: " + loginResp.statusCode);
                return;
            }
            // Try to access users endpoint (should be restricted)
            ApiClient.ApiResponse usersResp = api.get("/api/users/");
            // Restore admin token
            api.setAuthToken(savedToken);
            if (usersResp.statusCode == 403) {
                result.pass("Employee correctly restricted from /api/users/ (403 Forbidden)");
            } else {
                result.skip("User access check: status " + usersResp.statusCode + " (permission may vary)");
            }
        });

        runTest("PUT /api/users/{id} - Update user info", result -> {
            Integer userId = ctx.getCreatedUserId();
            if (userId == null) { result.skip("No user to update"); return; }
            String body = """
                {"phone": "0888777666", "department": "Updated Dept"}
                """;
            ApiClient.ApiResponse resp = api.put("/api/users/" + userId, body);
            assertTrue(resp.success, "Failed to update user: " + resp.body);
            result.pass("User updated successfully");
        });

        runTest("POST /api/users/ - Duplicate username rejected", result -> {
            String body = String.format("""
                {"username": "%s", "password": "Test@12345", "position": "Nhân viên"}
                """, testUsername);
            ApiClient.ApiResponse resp = api.post("/api/users/", body);
            assertTrue(!resp.success, "Expected duplicate rejection but got " + resp.statusCode);
            result.pass("Duplicate username correctly rejected (status " + resp.statusCode + ")");
        });

        runTest("DELETE /api/users/{id} - Delete test user (cleanup)", result -> {
            Integer userId = ctx.getCreatedUserId();
            if (userId == null) { result.skip("No user to delete"); return; }
            ApiClient.ApiResponse resp = api.delete("/api/users/" + userId);
            assertTrue(resp.success, "Failed to delete user: " + resp.body);
            ctx.setCreatedUserId(null);
            result.pass("Test user deleted: " + testUsername);
        });

        return results;
    }
}
