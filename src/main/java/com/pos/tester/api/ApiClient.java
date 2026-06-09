package com.pos.tester.api;

import com.google.gson.*;
import com.pos.tester.model.TestConfig;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;

public class ApiClient {
    private final TestConfig config;
    private final HttpClient httpClient;
    private String authToken;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ApiClient(TestConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .build();
    }

    public static class ApiResponse {
        public final int statusCode;
        public final String body;
        public final JsonElement json;
        public final boolean success;

        public ApiResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
            this.success = statusCode >= 200 && statusCode < 300;
            JsonElement parsed = null;
            try {
                parsed = JsonParser.parseString(body);
            } catch (Exception ignored) {}
            this.json = parsed;
        }

        public JsonObject jsonObject() {
            if (json != null && json.isJsonObject()) return json.getAsJsonObject();
            return new JsonObject();
        }

        public JsonArray jsonArray() {
            if (json != null && json.isJsonArray()) return json.getAsJsonArray();
            return new JsonArray();
        }

        public String getField(String field) {
            try {
                JsonObject obj = jsonObject();
                if (obj.has(field)) return obj.get(field).getAsString();
            } catch (Exception ignored) {}
            return null;
        }

        public int getIntField(String field) {
            try {
                JsonObject obj = jsonObject();
                if (obj.has(field)) return obj.get(field).getAsInt();
            } catch (Exception ignored) {}
            return -1;
        }

        public boolean hasField(String field) {
            try {
                return jsonObject().has(field);
            } catch (Exception ignored) {}
            return false;
        }
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public String getAuthToken() { return authToken; }

    public ApiResponse get(String endpoint) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + endpoint))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .GET();
        addAuthHeader(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new ApiResponse(response.statusCode(), response.body());
    }

    public ApiResponse post(String endpoint, String jsonBody) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + endpoint))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        addAuthHeader(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new ApiResponse(response.statusCode(), response.body());
    }

    public ApiResponse put(String endpoint, String jsonBody) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + endpoint))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody));
        addAuthHeader(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new ApiResponse(response.statusCode(), response.body());
    }

    public ApiResponse delete(String endpoint) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + endpoint))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .DELETE();
        addAuthHeader(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new ApiResponse(response.statusCode(), response.body());
    }

    public ApiResponse putForm(String endpoint, Map<String, String> params) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append(java.net.URLEncoder.encode(e.getKey(), java.nio.charset.StandardCharsets.UTF_8))
              .append("=")
              .append(java.net.URLEncoder.encode(e.getValue() != null ? e.getValue() : "", java.nio.charset.StandardCharsets.UTF_8));
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + endpoint))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .PUT(HttpRequest.BodyPublishers.ofString(sb.toString()));
        addAuthHeader(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new ApiResponse(response.statusCode(), response.body());
    }

    public ApiResponse postForm(String endpoint, Map<String, String> params) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append(java.net.URLEncoder.encode(e.getKey(), java.nio.charset.StandardCharsets.UTF_8))
              .append("=")
              .append(java.net.URLEncoder.encode(e.getValue() != null ? e.getValue() : "", java.nio.charset.StandardCharsets.UTF_8));
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + endpoint))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(sb.toString()));
        addAuthHeader(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new ApiResponse(response.statusCode(), response.body());
    }

    private void addAuthHeader(HttpRequest.Builder builder) {
        if (authToken != null && !authToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + authToken);
        }
    }

    public String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public ApiResponse login(String username, String password) throws Exception {
        String body = gson.toJson(Map.of("username", username, "password", password));
        ApiResponse resp = post("/api/auth/login", body);
        if (resp.success) {
            String token = resp.getField("access_token");
            if (token != null) setAuthToken(token);
        }
        return resp;
    }

    public boolean isConnected() {
        try {
            ApiResponse resp = get("/health");
            return resp.statusCode == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public String getBaseUrl() { return config.getBaseUrl(); }
}
