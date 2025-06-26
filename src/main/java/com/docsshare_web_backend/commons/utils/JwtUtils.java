package com.docsshare_web_backend.commons.utils;

import com.docsshare_web_backend.users.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class JwtUtils {
    private static final String SECRET_KEY = "bXlzZWNyZXRrZXlmb3Jqd3RzaG91bGRiZXZlcnlsb25nMTIzNDU2Nzg5MA==";
    private static final Duration TOKEN_EXPIRATION_TIME = Duration.of(1, ChronoUnit.DAYS);
    private static final Duration REFRESH_EXPIRATION_TIME = Duration.of(30, ChronoUnit.DAYS);
    public static final String TOKEN_NAME = "token";
    public static final String REFRESH_TOKEN_NAME = "refreshToken";
    public static final String TOKEN_PREFIX = "Bearer ";

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setAllowedClockSkewSeconds(60)
                .setSigningKey(getSignInKeys())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKeys() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Date getExpirationDate(String token) {
        return extractClaims(token, Claims::getExpiration);
    }

    public boolean isTokenExpired(String token) {
        return getExpirationDate(token)
                .before(Date.from(Instant.now()));
    }

    public String extractUsername(String jwt) {
        return extractClaims(jwt, Claims::getSubject);
    }

    public String extractEmail(String jwt) {
        return extractClaims(jwt, Claims::getSubject);
    }

    public <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (userDetails instanceof User user) {
            extraClaims.put("userId", user.getId());
            extraClaims.put("name", user.getName());
            extraClaims.put("email", user.getEmail());
            extraClaims.put("role", user.getUserType());
            extraClaims.put("status", user.getStatus());
        }
        return generateToken(extraClaims, userDetails);
    }


    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())  // Use getUsername() which returns email
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plus(TOKEN_EXPIRATION_TIME)))
                .signWith(getSignInKeys(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (userDetails instanceof User user) {
            extraClaims.put("userId", user.getId());
            extraClaims.put("role", user.getUserType());
            extraClaims.put("status", user.getStatus());
        }

        return generateRefreshToken(extraClaims, userDetails);
    }


    public String generateRefreshToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())  // Use getUsername() which returns email
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plus(REFRESH_EXPIRATION_TIME)))
                .signWith(getSignInKeys(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }
}
