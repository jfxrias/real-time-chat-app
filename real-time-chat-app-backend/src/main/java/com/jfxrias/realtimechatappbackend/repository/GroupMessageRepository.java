package com.jfxrias.realtimechatappbackend.repository;

import com.jfxrias.realtimechatappbackend.entity.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, String> {
    List<GroupMessage> findByTargetGroupUuid(String targetGroupUuid);
}
