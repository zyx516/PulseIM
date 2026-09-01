package com.pulseim.social;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
interface GroupMemberRepository extends JpaRepository<GroupMemberEntity,String>{
 List<GroupMemberEntity> findByUserId(String userId); List<GroupMemberEntity> findByGroupId(String groupId); Optional<GroupMemberEntity> findByGroupIdAndUserId(String groupId,String userId);
}