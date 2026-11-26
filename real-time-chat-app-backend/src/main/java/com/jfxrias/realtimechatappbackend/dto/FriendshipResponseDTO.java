package com.jfxrias.realtimechatappbackend.dto;

import java.time.LocalDateTime;

public record FriendshipResponseDTO(
        String userUuid,
        String name,
        String cpf,
        Boolean isAdmin,
        Boolean isActive,
        LocalDateTime createdAt
) {
}
