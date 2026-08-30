package com.pulseim.user;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserProfileRepository extends JpaRepository<UserProfileEntity, String> {
}
