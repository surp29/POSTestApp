package com.pos.tester.api;

import java.util.HashMap;
import java.util.Map;

public class TestContext {
    private final ApiClient apiClient;
    private final Map<String, Object> sharedData = new HashMap<>();

    // IDs created during tests for cleanup
    private Integer createdProductId;
    private Integer createdOrderId;
    private Integer createdInvoiceId;
    private Integer createdUserId;
    private Integer createdAccountId;
    private Integer createdWarehouseId;
    private Integer createdAreaId;
    private Integer createdShopId;
    private String createdDiscountCode;
    private Integer createdShipmentId;
    private Integer createdProductGroupId;

    public TestContext(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() { return apiClient; }

    public void set(String key, Object value) { sharedData.put(key, value); }
    public Object get(String key) { return sharedData.get(key); }
    public String getString(String key) {
        Object v = sharedData.get(key);
        return v != null ? v.toString() : null;
    }
    public Integer getInteger(String key) {
        Object v = sharedData.get(key);
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) try { return Integer.parseInt((String) v); } catch (Exception e) { return null; }
        return null;
    }

    public Integer getCreatedProductId() { return createdProductId; }
    public void setCreatedProductId(Integer id) { this.createdProductId = id; }
    public Integer getCreatedOrderId() { return createdOrderId; }
    public void setCreatedOrderId(Integer id) { this.createdOrderId = id; }
    public Integer getCreatedInvoiceId() { return createdInvoiceId; }
    public void setCreatedInvoiceId(Integer id) { this.createdInvoiceId = id; }
    public Integer getCreatedUserId() { return createdUserId; }
    public void setCreatedUserId(Integer id) { this.createdUserId = id; }
    public Integer getCreatedAccountId() { return createdAccountId; }
    public void setCreatedAccountId(Integer id) { this.createdAccountId = id; }
    public Integer getCreatedWarehouseId() { return createdWarehouseId; }
    public void setCreatedWarehouseId(Integer id) { this.createdWarehouseId = id; }
    public Integer getCreatedAreaId() { return createdAreaId; }
    public void setCreatedAreaId(Integer id) { this.createdAreaId = id; }
    public Integer getCreatedShopId() { return createdShopId; }
    public void setCreatedShopId(Integer id) { this.createdShopId = id; }
    public String getCreatedDiscountCode() { return createdDiscountCode; }
    public void setCreatedDiscountCode(String code) { this.createdDiscountCode = code; }
    public Integer getCreatedShipmentId() { return createdShipmentId; }
    public void setCreatedShipmentId(Integer id) { this.createdShipmentId = id; }
    public Integer getCreatedProductGroupId() { return createdProductGroupId; }
    public void setCreatedProductGroupId(Integer id) { this.createdProductGroupId = id; }

    public void reset() {
        sharedData.clear();
        createdProductId = null;
        createdOrderId = null;
        createdInvoiceId = null;
        createdUserId = null;
        createdAccountId = null;
        createdWarehouseId = null;
        createdAreaId = null;
        createdShopId = null;
        createdDiscountCode = null;
        createdShipmentId = null;
        createdProductGroupId = null;
    }
}
