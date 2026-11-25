package com.jfxrias.realtimechatappbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record EditMessageRequestDTO(
        @NotBlank(message = "Conteúdo da mensagem é obrigatório") String content
) {
}
