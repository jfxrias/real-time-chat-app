package com.jfxrias.realtimechatappbackend.dto;

public record MostGroupsResponseDTO(
        String userUuid,
        String name,
        Long totalGroups
) {
}
