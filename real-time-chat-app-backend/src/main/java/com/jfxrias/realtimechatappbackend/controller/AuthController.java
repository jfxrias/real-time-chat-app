package com.jfxrias.realtimechatappbackend.controller;

import com.jfxrias.realtimechatappbackend.dto.AuthResponseDTO;
import com.jfxrias.realtimechatappbackend.dto.LoginRequestDTO;
import com.jfxrias.realtimechatappbackend.dto.RegisterRequestDTO;
import com.jfxrias.realtimechatappbackend.dto.UserResponseDTO;
import com.jfxrias.realtimechatappbackend.entity.User;
import com.jfxrias.realtimechatappbackend.service.AuthService;
import com.jfxrias.realtimechatappbackend.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
