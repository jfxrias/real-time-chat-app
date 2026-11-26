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
            extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(analyticsService.getReceivedPercentageByUser());
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/users/most-friends")
    public ResponseEntity<?> getMostFriends(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(analyticsService.getUsersWithMostFriends());
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/groups/member-count")
    public ResponseEntity<?> getGroupMemberCount(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(analyticsService.getGroupMemberCount());
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/reports/top-reported")
    public ResponseEntity<?> getTopReported(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(analyticsService.getTopReportedUsers());
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/reports/common-reasons")
    public ResponseEntity<?> getCommonReasons(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(analyticsService.getCommonReportReasons());
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/users/most-groups")
    public ResponseEntity<?> getMostGroups(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(analyticsService.getUsersInMostGroups());
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/users/most-inactive")
    public ResponseEntity<?> getMostInactive(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(analyticsService.getMostInactiveUser());
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
            extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(analyticsService.getUserMessageStats(userUuid, start, end));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e);
        }
    }

    private String extractAuthenticatedUserUuid(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token nao informado");
        }
        String token = authorization.substring(7).trim();
        if (token.isBlank()) {
            throw new IllegalArgumentException("Token nao informado");
        }
        return jwtUtil.extractUserUuid(token);
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(IllegalArgumentException e) {
        boolean tokenError = e.getMessage().equals("Token invalido ou expirado")
                || e.getMessage().equals("Token nao informado");
        return tokenError
                ? ResponseEntity.status(401).body(Map.of("message", e.getMessage()))
                : ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}
