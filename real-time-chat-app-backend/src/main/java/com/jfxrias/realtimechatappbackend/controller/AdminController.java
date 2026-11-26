package com.jfxrias.realtimechatappbackend.controller;

import com.jfxrias.realtimechatappbackend.entity.Group;
import com.jfxrias.realtimechatappbackend.entity.User;
import com.jfxrias.realtimechatappbackend.service.AdminService;
import com.jfxrias.realtimechatappbackend.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;
    private final JwtUtil jwtUtil;

    public AdminController(AdminService adminService, JwtUtil jwtUtil) {
        this.adminService = adminService;
        this.jwtUtil = jwtUtil;
    }

    @PatchMapping("/users/{userUuid}/deactivate")
    public ResponseEntity<?> deactivateUser(
            @PathVariable String userUuid,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return handleUserStatusUpdate(userUuid, authorization, false);
    }

    @PatchMapping("/users/{userUuid}/reactivate")
    public ResponseEntity<?> reactivateUser(
            @PathVariable String userUuid,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return handleUserStatusUpdate(userUuid, authorization, true);
    }

    @PatchMapping("/groups/{groupUuid}/deactivate")
    public ResponseEntity<?> deactivateGroup(
            @PathVariable String groupUuid,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return handleGroupStatusUpdate(groupUuid, authorization, false);
    }

    @PatchMapping("/groups/{groupUuid}/reactivate")
    public ResponseEntity<?> reactivateGroup(
            @PathVariable String groupUuid,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return handleGroupStatusUpdate(groupUuid, authorization, true);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String adminUuid = extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(adminService.getAllUsers(adminUuid));
        } catch (IllegalArgumentException e) {
            boolean tokenError = e.getMessage().equals("Token inválido ou expirado")
                    || e.getMessage().equals("Token não informado");
            return tokenError
                    ? ResponseEntity.status(401).body(Map.of("message", e.getMessage()))
                    : ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/groups")
    public ResponseEntity<?> getAllGroups(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String adminUuid = extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(adminService.getAllGroups(adminUuid));
        } catch (IllegalArgumentException e) {
            boolean tokenError = e.getMessage().equals("Token inválido ou expirado")
                    || e.getMessage().equals("Token não informado");
            return tokenError
                    ? ResponseEntity.status(401).body(Map.of("message", e.getMessage()))
                    : ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private ResponseEntity<?> handleUserStatusUpdate(String userUuid, String authorization, boolean isActive) {
        try {
            String adminUuid = extractAuthenticatedUserUuid(authorization);
            User user = adminService.setUserActiveStatus(adminUuid, userUuid, isActive);
            return ResponseEntity.ok(Map.of(
                    "message", isActive ? "Conta reativada com sucesso" : "Conta desativada com sucesso",
                    "userUuid", user.getUserUuid(),
                    "isActive", user.getIsActive()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private ResponseEntity<?> handleGroupStatusUpdate(String groupUuid, String authorization, boolean isActive) {
        try {
            String adminUuid = extractAuthenticatedUserUuid(authorization);
            Group group = adminService.setGroupActiveStatus(adminUuid, groupUuid, isActive);
            return ResponseEntity.ok(Map.of(
                    "message", isActive ? "Grupo reativado com sucesso" : "Grupo desativado com sucesso",
                    "groupUuid", group.getGroupUuid(),
                    "status", group.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
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
