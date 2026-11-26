package com.jfxrias.realtimechatappbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReportRequestDTO(
        @NotBlank(message = "UUID do usuário reportado é obrigatório") String reportedUuid,
        @NotBlank(message = "UUID do grupo é obrigatório") String groupUuid,
        @NotBlank(message = "Motivo é obrigatório")
        @Size(max = 255, message = "Motivo deve ter no máximo 255 caracteres")
        String reason
) {
}
