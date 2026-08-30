package com.pulseim.user;

import com.pulseim.common.api.ApiResponse;
import com.pulseim.common.api.TraceIds;
import com.pulseim.common.security.BearerTokens;
import com.pulseim.common.security.JwtSupport;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserProfileRepository profiles;

    public UserController(UserProfileRepository profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/me")
    public ApiResponse<Profile> me(@RequestHeader("Authorization") String authorization,
                                   @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        JwtSupport.Session session = BearerTokens.require(authorization);
        UserProfileEntity profile = profiles.findById(session.userId())
                .orElseGet(() -> profiles.save(UserProfileEntity.defaultFor(session.userId())));
        return ApiResponse.ok(TraceIds.from(traceId), Profile.from(profile));
    }

    @PutMapping("/me")
    public ApiResponse<Profile> update(@RequestHeader("Authorization") String authorization,
                                       @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                       @RequestBody ProfileUpdate update) {
        JwtSupport.Session session = BearerTokens.require(authorization);
        UserProfileEntity profile = profiles.findById(session.userId())
                .orElseGet(() -> UserProfileEntity.defaultFor(session.userId()));
        profile.update(update.nickname(), update.avatarUrl(), update.color());
        return ApiResponse.ok(TraceIds.from(traceId), Profile.from(profiles.save(profile)));
    }

    public record Profile(String userId, String nickname, String avatarUrl, String color) {
        static Profile from(UserProfileEntity entity) {
            return new Profile(entity.userId(), entity.nickname(), entity.avatarUrl(), entity.color());
        }
    }

    public record ProfileUpdate(String nickname, String avatarUrl, String color) {
    }
}
