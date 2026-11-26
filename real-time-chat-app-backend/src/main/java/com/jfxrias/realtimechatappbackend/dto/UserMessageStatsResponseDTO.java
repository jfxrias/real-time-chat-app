package com.jfxrias.realtimechatappbackend.dto;

public record UserMessageStatsResponseDTO(
        String userUuid,
        String name,
        Long sentCount,
        Long receivedCount,
        Long totalCount
) {
}
