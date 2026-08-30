package com.pulseim.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_profiles")
class UserProfileEntity {
    @Id
    @Column(name = "user_id", length = 80)
    private String userId;
    @Column(nullable = false, length = 80)
    private String nickname;
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    @Column(nullable = false, length = 16)
    private String color;

    protected UserProfileEntity() {
    }

    UserProfileEntity(String userId, String nickname, String avatarUrl, String color) {
        this.userId = userId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl == null ? "" : avatarUrl;
        this.color = color == null || color.isBlank() ? "#246BEB" : color;
    }

    static UserProfileEntity defaultFor(String userId) {
        return new UserProfileEntity(userId, "Pulse user", "", "#246BEB");
    }

    void update(String nickname, String avatarUrl, String color) {
        this.nickname = nickname == null || nickname.isBlank() ? this.nickname : nickname;
        this.avatarUrl = avatarUrl == null ? "" : avatarUrl;
        this.color = color == null || color.isBlank() ? this.color : color;
    }

    String userId() { return userId; }
    String nickname() { return nickname; }
    String avatarUrl() { return avatarUrl; }
    String color() { return color; }
}
