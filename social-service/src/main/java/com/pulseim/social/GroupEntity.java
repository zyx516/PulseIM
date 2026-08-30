package com.pulseim.social;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "im_groups")
class GroupEntity {
    @Id
    private String id;
    @Column(nullable = false, length = 80)
    private String name;
    @Column(name = "owner_id", nullable = false, length = 80)
    private String ownerId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GroupEntity() {
    }

    GroupEntity(String id, String name, String ownerId, Instant createdAt) {
        this.id = id;
        this.name = name == null || name.isBlank() ? "新群聊" : name;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    String id() { return id; }
    String name() { return name; }
    String ownerId() { return ownerId; }
    Instant createdAt() { return createdAt; }
}
