package com.jfxrias.realtimechatappbackend.repository;

import com.jfxrias.realtimechatappbackend.entity.PrivateMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, String> {
    @Query("""
            select pm.messageUuid
            from PrivateMessage pm
            where (pm.message.senderUuid = :userA and pm.receiverUuid = :userB)
               or (pm.message.senderUuid = :userB and pm.receiverUuid = :userA)
            """)
    List<String> findConversationMessageUuids(String userA, String userB);
}
