package com.jfxrias.realtimechatappbackend.dto;

import java.time.LocalDateTime;

public record MostInactiveResponseDTO(
        String userUuid,
        String name,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt
) {
}
