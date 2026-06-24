package com.blog.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiration;

    public JwtUtil(@Value("${app.jwt.secret:blog-system-secret-key-min-256-bits-long!!}") String secret,
                   @Value("${app.jwt.expiration:86400000}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, null, null);
    }

    public String generateToken(Long userId, String username, String role, String nickname, String avatar) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration));
        if (nickname != null) builder.claim("nickname", nickname);
        if (avatar != null) builder.claim("avatar", avatar);
        return builder.signWith(key).compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public String getUsername(Claims claims) {
        return claims.get("username", String.class);
    }

    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public String getNickname(Claims claims) {
        return claims.get("nickname", String.class);
    }

    public String getAvatar(Claims claims) {
        return claims.get("avatar", String.class);
    }

    /** Convenience: parse token and return userId. */
    public Long getUserIdFromToken(String token) {
        return getUserId(parseToken(token));
    }

    /** Convenience: parse token and return nickname. */
    public String getNicknameFromToken(String token) {
        return getNickname(parseToken(token));
    }
}
