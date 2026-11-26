package com.jfxrias.realtimechatappbackend.repository;

import com.jfxrias.realtimechatappbackend.entity.Friendship;
import com.jfxrias.realtimechatappbackend.entity.FriendshipId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendshipRepository extends JpaRepository<Friendship, FriendshipId> {
}
