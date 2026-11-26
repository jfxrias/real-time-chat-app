package com.jfxrias.realtimechatappbackend.dto;

public record TopReportedResponseDTO(
        String userUuid,
        String name,
        Long totalReports
) {
}
