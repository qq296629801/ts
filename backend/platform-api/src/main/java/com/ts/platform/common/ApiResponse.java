package com.ts.platform.common;

import java.time.Instant;

public record ApiResponse<T>(int code, String message, T data, long timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "success", data, Instant.now().getEpochSecond());
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, Instant.now().getEpochSecond());
    }
}
