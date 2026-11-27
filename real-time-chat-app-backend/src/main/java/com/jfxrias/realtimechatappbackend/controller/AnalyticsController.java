package com.jfxrias.realtimechatappbackend.controller;

import com.jfxrias.realtimechatappbackend.service.AnalyticsService;
import com.jfxrias.realtimechatappbackend.util.JwtUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final JwtUtil jwtUtil;

    public AnalyticsController(AnalyticsService analyticsService, JwtUtil jwtUtil) {
        this.analyticsService = analyticsService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/messages/received-percentage")
    public ResponseEntity<?> getReceivedPercentage(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String adminUuid = extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(analyticsService.getReceivedPercentageByUser(adminUuid));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/users/most-friends")
    public ResponseEntity<?> getMostFriends(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String adminUuid = extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(analyticsService.getUsersWithMostFriends(adminUuid));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/groups/member-count")
    public ResponseEntity<?> getGroupMemberCount(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String adminUuid = extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(analyticsService.getGroupMemberCount(adminUuid));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/reports/top-reported")
    public ResponseEntity<?> getTopReported(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String adminUuid = extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(analyticsService.getTopReportedUsers(adminUuid));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/reports/common-reasons")
    public ResponseEntity<?> getCommonReasons(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String adminUuid = extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(analyticsService.getCommonReportReasons(adminUuid));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/users/most-groups")
    public ResponseEntity<?> getMostGroups(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String adminUuid = extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(analyticsService.getUsersInMostGroups(adminUuid));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/users/most-inactive")
    public ResponseEntity<?> getMostInactive(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String adminUuid = extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(analyticsService.getMostInactiveUser(adminUuid));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/users/{userUuid}/message-stats")
    public ResponseEntity<?> getUserMessageStats(
            @PathVariable String userUuid,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String adminUuid = extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(analyticsService.getUserMessageStats(adminUuid, userUuid, start, end));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e);
        }
    }

    private String extractAuthenticatedUserUuid(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token não informado");
        }
        String token = authorization.substring(7).trim();
        if (token.isBlank()) {
            throw new IllegalArgumentException("Token não informado");
        }
        return jwtUtil.extractUserUuid(token);
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(IllegalArgumentException e) {
        boolean tokenError = e.getMessage().equals("Token inválido ou expirado")
                || e.getMessage().equals("Token não informado");
        if (tokenError) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        }
        if (e.getMessage().equals("Acesso exclusivo para administradores")) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        }
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}
