package com.freshmart.smartstock.common.api;

/**
 * Unified response envelope for every endpoint (success and error):
 * { "status": 200, "message": "success", "data": {...} }
 */
public record ApiResponse<T>(int status, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(201, "created", data);
    }

    public static <T> ApiResponse<T> error(int status, String message, T data) {
        return new ApiResponse<>(status, message, data);
    }
}
