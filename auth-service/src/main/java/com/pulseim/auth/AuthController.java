package com.pulseim.auth;

import com.pulseim.common.api.ApiResponse;
import com.pulseim.common.api.TraceIds;
import com.pulseim.common.security.JwtSupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountRepository accounts;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TokenView> register(@RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                           @Valid @RequestBody Credentials request) {
        AccountEntity account = new AccountEntity("u-" + UUID.randomUUID(), request.account(),
                passwordEncoder.encode(request.password()));
        try {
            accounts.saveAndFlush(account);
        } catch (DataIntegrityViolationException duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account already exists");
        }
        return ApiResponse.ok(TraceIds.from(traceId), tokens(account.userId(), request.deviceId()));
    }

    @PostMapping("/login")
    public ApiResponse<TokenView> login(@RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                        @Valid @RequestBody Credentials request) {
        AccountEntity account = accounts.findByAccount(request.account())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid account or password"));
        if (!passwordEncoder.matches(request.password(), account.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid account or password");
        }
        return ApiResponse.ok(TraceIds.from(traceId), tokens(account.userId(), request.deviceId()));
    }

    private TokenView tokens(String userId, String deviceId) {
        String safeDeviceId = deviceId == null || deviceId.isBlank() ? "web" : deviceId;
        return new TokenView(userId, JwtSupport.issue(userId, safeDeviceId, Duration.ofMinutes(30)),
                JwtSupport.issue(userId, safeDeviceId, Duration.ofDays(14)));
    }

    public record Credentials(@NotBlank @Size(max = 32) String account,
                              @NotBlank @Size(min = 6, max = 72) String password,
                              String deviceId) {
    }

    public record TokenView(String userId, String accessToken, String refreshToken) {
    }
}
