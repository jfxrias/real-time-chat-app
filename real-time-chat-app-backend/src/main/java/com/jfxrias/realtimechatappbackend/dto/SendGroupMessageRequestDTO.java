package com.jfxrias.realtimechatappbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SendGroupMessageRequestDTO(
        @NotEmpty(message = "Informe ao menos um grupo") List<String> groupUuids,
        @NotBlank(message = "Conteúdo da mensagem é obrigatório") String content
) {
}
