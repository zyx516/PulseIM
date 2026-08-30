package com.pulseim.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface AccountRepository extends JpaRepository<AccountEntity, String> {
    Optional<AccountEntity> findByAccount(String account);
}
