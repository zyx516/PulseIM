package com.pulseim.common.security;

public final class BearerTokens {
    private BearerTokens() { }

    public static JwtSupport.Session require(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing Bearer access token");
        }
        return JwtSupport.verify(authorization.substring(7));
    }
}
