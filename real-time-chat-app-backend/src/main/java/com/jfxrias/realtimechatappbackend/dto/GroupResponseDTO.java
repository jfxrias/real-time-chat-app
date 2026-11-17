package com.jfxrias.realtimechatappbackend.dto;

import java.time.LocalDateTime;

public record GroupResponseDTO(
        String groupUuid,
        String groupName,
        String creatorUuid,
        String status,
        LocalDateTime createdAt
) {
}
