package com.jfxrias.realtimechatappbackend.repository;

import com.jfxrias.realtimechatappbackend.entity.GroupMember;
import com.jfxrias.realtimechatappbackend.entity.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {
    List<GroupMember> findByIdUserUuid(String userUuid);
    List<GroupMember> findByIdGroupUuid(String groupUuid);
}
