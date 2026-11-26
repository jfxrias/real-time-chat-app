package com.jfxrias.realtimechatappbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateGroupRequestDTO(
        @NotBlank(message = "Nome do grupo é obrigatório")
        String groupName
) {
}
