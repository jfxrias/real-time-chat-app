package com.jfxrias.realtimechatappbackend.dto;

public record MostFriendsResponseDTO(
        String userUuid,
        String name,
        Long totalFriends
) {
}
