package com.freshmart.smartstock.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Single business exception type carrying an HTTP status and optional payload (e.g. list of
 * products with insufficient stock). GlobalExceptionHandler turns it into the standard envelope.
 *
 * Use the factories: ApiException.notFound("Product 12 not found"), .conflict(msg, data), ...
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final transient Object data;

    public ApiException(HttpStatus status, String message, Object data) {
        super(message);
        this.status = status;
        this.data = data;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Object getData() {
        return data;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, null);
    }

    public static ApiException badRequest(String message, Object data) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, data);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message, null);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message, null);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message, null);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message, null);
    }

    public static ApiException conflict(String message, Object data) {
        return new ApiException(HttpStatus.CONFLICT, message, data);
    }

    public static ApiException serviceUnavailable(String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, message, null);
    }

    /** Placeholder used by skeleton services. Replace with real logic, then delete the call. */
    public static ApiException notImplemented(String owner) {
        return new ApiException(HttpStatus.NOT_IMPLEMENTED, "Chưa triển khai - TODO[" + owner + "]", null);
    }
}
