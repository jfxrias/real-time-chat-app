package com.jfxrias.realtimechatappbackend.util;

import com.jfxrias.realtimechatappbackend.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    private static final String SECRET = "segredo_super_secreto_1234567890";
    private static final long EXPIRATION = 1000 * 60 * 60 * 24L;

    public String generateToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(user.getUserUuid())
                .claim("cpf", user.getCpf())
                .claim("isAdmin", user.getIsAdmin())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
