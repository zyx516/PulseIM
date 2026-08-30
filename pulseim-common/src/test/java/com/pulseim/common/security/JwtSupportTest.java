package com.pulseim.common.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtSupportTest {
    @Test
    void issuesAndReadsUserAndDeviceClaims() {
        String token = JwtSupport.issue("u-1", "web-test", Duration.ofMinutes(1));

        JwtSupport.Session session = JwtSupport.verify(token);

        assertThat(session.userId()).isEqualTo("u-1");
        assertThat(session.deviceId()).isEqualTo("web-test");
    }
}
