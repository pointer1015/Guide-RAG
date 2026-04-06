package com.guiderag.common.utils;

import com.guiderag.common.constant.AuthConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtils {

    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(AuthConstants.JWT_SECRET.getBytes(StandardCharsets.UTF_8));

    public static String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + AuthConstants.JWT_EXPIRATION);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(SECRET_KEY)
                .compact();
    }

    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public static Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null && claims.getSubject() != null) {
            return Long.parseLong(claims.getSubject());
        }
        return null;
    }
    
    public static boolean validateToken(String token) {
        return parseToken(token) != null;
    }
}
