package com.pulseim.common.ws;

import java.time.Instant;

public record WsEvent(String version, String requestId, String type, Object data, Instant timestamp) {
    public static WsEvent of(String requestId, String type, Object data) {
        return new WsEvent(WsEnvelope.VERSION, requestId, type, data, Instant.now());
    }
}
