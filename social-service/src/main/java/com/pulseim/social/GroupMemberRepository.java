package com.pulseim.social;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, String> {
    List<GroupMemberEntity> findByUserId(String userId);
    List<GroupMemberEntity> findByGroupId(String groupId);
}
