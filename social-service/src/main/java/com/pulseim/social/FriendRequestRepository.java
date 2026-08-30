package com.pulseim.social;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface FriendRequestRepository extends JpaRepository<FriendRequestEntity, String> {
    List<FriendRequestEntity> findByToUserIdAndStatus(String toUserId, String status);
}
