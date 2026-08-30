package com.pulseim.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public final class JwtSupport {
    private static final String DEFAULT_SECRET = "pulseim-demo-secret-change-me-before-any-real-deployment-2026";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(
            System.getenv().getOrDefault("PULSEIM_JWT_SECRET", DEFAULT_SECRET).getBytes(StandardCharsets.UTF_8));

    private JwtSupport() { }

    public static String issue(String userId, String deviceId, Duration lifetime) {
        Instant now = Instant.now();
        return Jwts.builder().subject(userId).claim("deviceId", deviceId)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(lifetime))).signWith(KEY).compact();
    }

    public static Session verify(String token) {
        Claims claims = Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token).getPayload();
        return new Session(claims.getSubject(), claims.get("deviceId", String.class));
    }

    public record Session(String userId, String deviceId) { }
}
