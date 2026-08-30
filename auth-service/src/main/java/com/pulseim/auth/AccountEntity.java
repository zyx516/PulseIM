package com.pulseim.auth;

import jakarta.persistence.*;

@Entity
@Table(name = "accounts", uniqueConstraints = @UniqueConstraint(name = "uk_account", columnNames = "account"))
class AccountEntity {
    @Id
    @Column(name = "user_id", length = 80)
    private String userId;
    @Column(nullable = false, length = 32)
    private String account;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    protected AccountEntity() {
    }

    AccountEntity(String userId, String account, String passwordHash) {
        this.userId = userId;
        this.account = account;
        this.passwordHash = passwordHash;
    }

    String userId() { return userId; }
    String account() { return account; }
    String passwordHash() { return passwordHash; }
}
