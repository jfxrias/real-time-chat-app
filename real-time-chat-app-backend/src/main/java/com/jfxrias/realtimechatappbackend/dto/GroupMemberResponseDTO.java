package com.jfxrias.realtimechatappbackend.dto;

import java.time.LocalDateTime;

public record GroupMemberResponseDTO(
        String userUuid,
        String name,
        String role,
        LocalDateTime joinedAt
) {
}
