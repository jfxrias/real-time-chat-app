package com.jfxrias.realtimechatappbackend.dto;

public record GroupMemberCountResponseDTO(
        String groupUuid,
        String groupName,
        Long totalUsers
) {
}
