package com.jfxrias.realtimechatappbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateNameRequestDTO(
        @NotBlank(message = "Nome é obrigatório") String name
) {
}
