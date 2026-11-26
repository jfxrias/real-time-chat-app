package com.jfxrias.realtimechatappbackend.dto;

import java.math.BigDecimal;

public record ReceivedPercentageResponseDTO(
        String userUuid,
        String name,
        Long receivedCount,
        BigDecimal percentage
) {
}
