package com.pulseim.user;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class UserDataInitializer {
    @Bean
    CommandLineRunner seedProfiles(UserProfileRepository profiles) {
        return args -> {
            profiles.findById("u-pulse").orElseGet(() -> profiles.save(new UserProfileEntity("u-pulse", "Pulse", "", "#246BEB")));
            profiles.findById("u-ava").orElseGet(() -> profiles.save(new UserProfileEntity("u-ava", "安然", "", "#8D73EE")));
        };
    }
}
