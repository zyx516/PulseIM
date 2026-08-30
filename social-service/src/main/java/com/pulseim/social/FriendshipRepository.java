package com.pulseim.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface FriendshipRepository extends JpaRepository<FriendshipEntity, String> {
    List<FriendshipEntity> findByUserId(String userId);

    default void upsert(String left, String right) {
        if (findByUserIdAndFriendUserId(left, right).isEmpty()) {
            save(new FriendshipEntity("f-" + UUID.randomUUID(), left, right, Instant.now()));
        }
        if (findByUserIdAndFriendUserId(right, left).isEmpty()) {
            save(new FriendshipEntity("f-" + UUID.randomUUID(), right, left, Instant.now()));
        }
    }

    List<FriendshipEntity> findByUserIdAndFriendUserId(String userId, String friendUserId);

    @Transactional
    @Modifying
    @Query("delete from FriendshipEntity f where (f.userId = :left and f.friendUserId = :right) or (f.userId = :right and f.friendUserId = :left)")
    void deletePair(String left, String right);
}
