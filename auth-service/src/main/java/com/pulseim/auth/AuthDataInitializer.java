package com.pulseim.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
class AuthDataInitializer {
    @Bean
    CommandLineRunner seedAccounts(AccountRepository accounts) {
        return args -> {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            if (accounts.findByAccount("pulse").isEmpty()) {
                accounts.save(new AccountEntity("u-pulse", "pulse", encoder.encode("pulse123")));
            }
            if (accounts.findByAccount("ava").isEmpty()) {
                accounts.save(new AccountEntity("u-ava", "ava", encoder.encode("ava123")));
            }
        };
    }
}
