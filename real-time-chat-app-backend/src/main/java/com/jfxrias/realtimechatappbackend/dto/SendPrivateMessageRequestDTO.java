package com.jfxrias.realtimechatappbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record SendPrivateMessageRequestDTO(
        @NotBlank(message = "UUID do destinatário é obrigatório") String receiverUuid,
        @NotBlank(message = "Conteúdo da mensagem é obrigatório") String content
) {
}
