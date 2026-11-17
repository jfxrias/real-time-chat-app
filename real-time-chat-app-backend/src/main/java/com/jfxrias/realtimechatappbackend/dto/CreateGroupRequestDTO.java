package com.jfxrias.realtimechatappbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateGroupRequestDTO(
        @NotBlank(message = "Nome do grupo é obrigatório") String groupName,
        String creatorUuid,
        String userUuid
) {
}
