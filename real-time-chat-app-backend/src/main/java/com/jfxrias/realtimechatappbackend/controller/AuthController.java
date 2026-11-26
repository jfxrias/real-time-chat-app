package com.jfxrias.realtimechatappbackend.controller;

import com.jfxrias.realtimechatappbackend.dto.AuthResponseDTO;
import com.jfxrias.realtimechatappbackend.dto.FriendshipResponseDTO;
import com.jfxrias.realtimechatappbackend.dto.LoginRequestDTO;
import com.jfxrias.realtimechatappbackend.dto.RegisterRequestDTO;
import com.jfxrias.realtimechatappbackend.dto.CreateFriendshipRequestDTO;
import com.jfxrias.realtimechatappbackend.dto.UpdateNameRequestDTO;
import com.jfxrias.realtimechatappbackend.dto.UpdatePasswordRequestDTO;
import com.jfxrias.realtimechatappbackend.dto.UserResponseDTO;
import com.jfxrias.realtimechatappbackend.entity.Friendship;
import com.jfxrias.realtimechatappbackend.entity.User;
import com.jfxrias.realtimechatappbackend.service.AuthService;
import com.jfxrias.realtimechatappbackend.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request,
                                     @RequestHeader(value = "admin-uuid", required = false) String adminUuid) {
        try {
            User adminUser = adminUuid != null ? authService.findById(adminUuid) : null;
            User user = authService.register(request, adminUser);
            return ResponseEntity.ok(toResponse(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        try {
            User user = authService.login(request.cpf(), request.password());
            String token = jwtUtil.generateToken(user);
            return ResponseEntity.ok(new AuthResponseDTO(token, toResponse(user)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/name")
    public ResponseEntity<?> updateName(
            @Valid @RequestBody UpdateNameRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String authenticatedUserUuid = extractAuthenticatedUserUuid(authorization);
            User user = authService.updateName(authenticatedUserUuid, request.name());
            return ResponseEntity.ok(toResponse(user));
        } catch (IllegalArgumentException e) {
            boolean tokenError = e.getMessage().equals("Token inválido ou expirado")
                    || e.getMessage().equals("Token não informado");
            return tokenError
                    ? ResponseEntity.status(401).body(Map.of("message", e.getMessage()))
                    : ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/password")
    public ResponseEntity<?> updatePassword(
            @Valid @RequestBody UpdatePasswordRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String authenticatedUserUuid = extractAuthenticatedUserUuid(authorization);
            authService.updatePassword(authenticatedUserUuid, request.password());
            return ResponseEntity.ok(Map.of("message", "Senha atualizada com sucesso"));
        } catch (IllegalArgumentException e) {
            boolean tokenError = e.getMessage().equals("Token inválido ou expirado")
                    || e.getMessage().equals("Token não informado");
            return tokenError
                    ? ResponseEntity.status(401).body(Map.of("message", e.getMessage()))
                    : ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/friendships")
    public ResponseEntity<?> createFriendship(
            @Valid @RequestBody CreateFriendshipRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String authenticatedUserUuid = extractAuthenticatedUserUuid(authorization);
            Friendship friendship = authService.addFriendship(authenticatedUserUuid, request.userUuid());
            return ResponseEntity.ok(Map.of(
                    "message", "Amizade criada com sucesso",
                    "firstUserUuid", friendship.getId().getFirstUserUuid(),
                    "secondUserUuid", friendship.getId().getSecondUserUuid(),
                    "createdAt", friendship.getCreatedAt()
            ));
        } catch (IllegalArgumentException e) {
            boolean tokenError = e.getMessage().equals("Token inválido ou expirado")
                    || e.getMessage().equals("Token não informado");
            return tokenError
                    ? ResponseEntity.status(401).body(Map.of("message", e.getMessage()))
                    : ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            String authenticatedUserUuid = extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(authService.getAuthenticatedUser(authenticatedUserUuid));
        } catch (IllegalArgumentException e) {
            boolean tokenError = e.getMessage().equals("Token inválido ou expirado")
                    || e.getMessage().equals("Token não informado");
            return tokenError
                    ? ResponseEntity.status(401).body(Map.of("message", e.getMessage()))
                    : ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> users(@RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            extractAuthenticatedUserUuid(authorization);
            return ResponseEntity.ok(authService.listActiveUsers());
        } catch (IllegalArgumentException e) {
            boolean tokenError = e.getMessage().equals("Token inválido ou expirado")
                    || e.getMessage().equals("Token não informado");
            return tokenError
                    ? ResponseEntity.status(401).body(Map.of("message", e.getMessage()))
                    : ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/friendships")
    public ResponseEntity<?> friendships(@RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            String authenticatedUserUuid = extractAuthenticatedUserUuid(authorization);
            List<FriendshipResponseDTO> friendships = authService.listFriendships(authenticatedUserUuid);
            return ResponseEntity.ok(friendships);
        } catch (IllegalArgumentException e) {
            boolean tokenError = e.getMessage().equals("Token inválido ou expirado")
                    || e.getMessage().equals("Token não informado");
            return tokenError
                    ? ResponseEntity.status(401).body(Map.of("message", e.getMessage()))
                    : ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/friendships/{userUuid}")
    public ResponseEntity<?> deleteFriendship(
            @PathVariable String userUuid,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String authenticatedUserUuid = extractAuthenticatedUserUuid(authorization);
            authService.deleteFriendship(authenticatedUserUuid, userUuid);
            return ResponseEntity.ok(Map.of("message", "Amizade removida com sucesso"));
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

    private UserResponseDTO toResponse(User user) {
        return new UserResponseDTO(
                user.getUserUuid(),
                user.getName(),
                user.getCpf(),
                user.getIsAdmin(),
                user.getIsActive()
        );
    }
}
