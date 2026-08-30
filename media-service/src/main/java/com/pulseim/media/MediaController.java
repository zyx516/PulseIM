package com.pulseim.media;

import com.pulseim.common.api.ApiResponse;
import com.pulseim.common.api.TraceIds;
import com.pulseim.common.security.BearerTokens;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
public class MediaController {
    private final MediaRepository media;

    public MediaController(MediaRepository media) {
        this.media = media;
    }

    @PostMapping("/uploads")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UploadView> createUpload(@RequestHeader("Authorization") String authorization,
                                                @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                                @Valid @RequestBody CreateUploadCommand command) {
        String ownerId = BearerTokens.require(authorization).userId();
        MediaObjectEntity object = media.save(new MediaObjectEntity("media-" + UUID.randomUUID(), ownerId,
                command.fileName(), command.contentType(), command.sizeBytes(), Instant.now()));
        return ApiResponse.ok(TraceIds.from(traceId), UploadView.from(object));
    }

    @GetMapping("/{mediaId}")
    public ApiResponse<MediaView> get(@RequestHeader("Authorization") String authorization,
                                      @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                      @PathVariable String mediaId) {
        BearerTokens.require(authorization);
        return ApiResponse.ok(TraceIds.from(traceId), media.findById(mediaId).map(MediaView::from).orElseThrow());
    }

    public record CreateUploadCommand(@NotBlank String fileName, @NotBlank String contentType, long sizeBytes) {
    }

    public record UploadView(String mediaId, String uploadUrl, Instant expiresAt) {
        static UploadView from(MediaObjectEntity entity) {
            return new UploadView(entity.id(), "/dev-upload/" + entity.id(), Instant.now().plus(Duration.ofMinutes(10)));
        }
    }

    public record MediaView(String mediaId, String ownerId, String fileName, String contentType, long sizeBytes,
                            String status, Instant createdAt) {
        static MediaView from(MediaObjectEntity entity) {
            return new MediaView(entity.id(), entity.ownerId(), entity.fileName(), entity.contentType(),
                    entity.sizeBytes(), entity.status(), entity.createdAt());
        }
    }
}
