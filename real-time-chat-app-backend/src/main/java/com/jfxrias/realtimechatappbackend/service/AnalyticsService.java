package com.jfxrias.realtimechatappbackend.service;

import com.jfxrias.realtimechatappbackend.dto.*;
import com.jfxrias.realtimechatappbackend.repository.AnalyticsRepository;
import com.jfxrias.realtimechatappbackend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnalyticsService {
    private final AnalyticsRepository analyticsRepository;
    private final UserRepository userRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository, UserRepository userRepository) {
        this.analyticsRepository = analyticsRepository;
        this.userRepository = userRepository;
    }

    public List<ReceivedPercentageResponseDTO> getReceivedPercentageByUser() {
        return analyticsRepository.findReceivedPercentageByUser();
    }

    public List<MostFriendsResponseDTO> getUsersWithMostFriends() {
        return analyticsRepository.findUsersWithMostFriends();
    }

    public List<GroupMemberCountResponseDTO> getGroupMemberCount() {
        return analyticsRepository.findGroupMemberCount();
    }

    public List<TopReportedResponseDTO> getTopReportedUsers() {
        return analyticsRepository.findTopReportedUsers();
    }

    public List<CommonReasonResponseDTO> getCommonReportReasons() {
        return analyticsRepository.findCommonReportReasons();
    }

    public List<MostGroupsResponseDTO> getUsersInMostGroups() {
        return analyticsRepository.findUsersInMostGroups();
    }

    public MostInactiveResponseDTO getMostInactiveUser() {
        return analyticsRepository.findMostInactiveUser()
                .orElseThrow(() -> new IllegalArgumentException("Nenhum usuario encontrado"));
    }

    public UserMessageStatsResponseDTO getUserMessageStats(String userUuid, LocalDateTime start, LocalDateTime end) {
        String normalizedUserUuid = normalizeRequired(userUuid, "UUID do usuario e obrigatorio");
        if (start == null || end == null) {
            throw new IllegalArgumentException("Parametros start e end sao obrigatorios");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start nao pode ser posterior a end");
        }

        userRepository.findById(normalizedUserUuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));

        return analyticsRepository.findUserMessageStats(normalizedUserUuid, start, end)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
    }

    private String normalizeRequired(String value, String message) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }
}
