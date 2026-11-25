package com.jfxrias.realtimechatappbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record AddGroupMemberRequestDTO(
        @NotBlank(message = "UUID do usuário é obrigatório") String userUuid,
        String role
) {
}
