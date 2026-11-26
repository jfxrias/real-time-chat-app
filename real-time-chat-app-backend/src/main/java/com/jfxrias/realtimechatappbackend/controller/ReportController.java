package com.jfxrias.realtimechatappbackend.controller;

import com.jfxrias.realtimechatappbackend.dto.CreateReportRequestDTO;
import com.jfxrias.realtimechatappbackend.entity.Report;
import com.jfxrias.realtimechatappbackend.service.ReportService;
import com.jfxrias.realtimechatappbackend.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/reports")
public class ReportController {
    private final ReportService reportService;
    private final JwtUtil jwtUtil;

    public ReportController(ReportService reportService, JwtUtil jwtUtil) {
        this.reportService = reportService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<?> createReport(
            @Valid @RequestBody CreateReportRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String reporterUuid = extractAuthenticatedUserUuid(authorization);
            Report report = reportService.createReport(
                    reporterUuid,
                    request.reportedUuid(),
                    request.groupUuid(),
                    request.reason()
            );

            return ResponseEntity.ok(Map.of(
                    "reportUuid", report.getReportUuid(),
                    "reporterUuid", report.getReporterUuid(),
                    "reportedUuid", report.getReportedUuid(),
                    "groupUuid", report.getGroupUuid(),
                    "reason", report.getReason(),
                    "reportedAt", report.getReportedAt(),
                    "message", "Usuário reportado com sucesso"
            ));
        } catch (IllegalArgumentException e) {
            boolean tokenError = e.getMessage().equals("Token inválido ou expirado")
                    || e.getMessage().equals("Token não informado");
            return tokenError
                    ? ResponseEntity.status(401).body(Map.of("message", e.getMessage()))
                    : ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
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
}
