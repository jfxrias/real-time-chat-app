package com.jfxrias.realtimechatappbackend.dto;

public record UserResponseDTO(
        String userUuid,
        String name,
        String cpf,
        Boolean isAdmin,
        Boolean isActive
) {
}
