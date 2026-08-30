package com.pulseim.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "media_objects")
class MediaObjectEntity {
    @Id
    private String id;
    @Column(name = "owner_id", nullable = false, length = 80)
    private String ownerId;
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MediaObjectEntity() {
    }

    MediaObjectEntity(String id, String ownerId, String fileName, String contentType, long sizeBytes, Instant createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.status = "PENDING_UPLOAD";
        this.createdAt = createdAt;
    }

    String id() { return id; }
    String ownerId() { return ownerId; }
    String fileName() { return fileName; }
    String contentType() { return contentType; }
    long sizeBytes() { return sizeBytes; }
    String status() { return status; }
    Instant createdAt() { return createdAt; }
}
