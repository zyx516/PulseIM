package com.pulseim.common.api;

import java.util.UUID;

public final class TraceIds {
    private TraceIds() { }

    public static String from(String traceId) {
        return traceId == null || traceId.isBlank() ? UUID.randomUUID().toString() : traceId;
    }
}
