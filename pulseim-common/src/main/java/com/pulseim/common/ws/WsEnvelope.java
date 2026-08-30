package com.pulseim.common.ws;

import com.fasterxml.jackson.databind.JsonNode;

/** Versioned application-level frame carried in a WebSocket text frame. */
public record WsEnvelope(String version, String requestId, String command, JsonNode data) {
    public static final String VERSION = "1";
}
