package com.pulseim.message;
import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param; import java.util.*;
interface MessageRepository extends JpaRepository<MessageEntity,String>{
 Optional<MessageEntity> findBySenderIdAndClientMessageId(String senderId,String clientMessageId);
 List<MessageEntity> findTop50ByConversationIdAndSequenceGreaterThanOrderBySequenceAsc(String conversationId,long sequence);
 @Query("select m from MessageEntity m where m.senderId=:userId or m.toUserId=:userId order by m.createdAt desc") List<MessageEntity> findTop200RelatedTo(@Param("userId")String userId);
}