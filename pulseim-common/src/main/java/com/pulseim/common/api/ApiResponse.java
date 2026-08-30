package com.pulseim.common.api;

import java.time.Instant;

/** Stable envelope shared by the HTTP services. */
public record ApiResponse<T>(String traceId, T data, Instant timestamp) {
    public static <T> ApiResponse<T> ok(String traceId, T data) {
        return new ApiResponse<>(traceId, data, Instant.now());
    }
}
