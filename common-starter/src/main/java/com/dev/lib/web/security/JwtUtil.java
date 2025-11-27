package com.dev.lib.web.security;

import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.security.TokenService;
import com.dev.lib.security.AuthenticateService;
import com.dev.lib.security.util.UserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil implements TokenService, InitializingBean {

    private final AppSecurityProperties securityProperties;
    private final AuthenticateService userDetailService;
    private SecretKey secretKey;

    @Override
    public void afterPropertiesSet() {
        // HMAC-SHA256 要求密钥至少 32 字节
        byte[] keyBytes = securityProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(Optional.ofNullable(userDetails.getExpireTime())
                .orElse(now.getTime()) + securityProperties.getExpiration());

        log.info("Generating token for user: {}", userDetails.getUsername());

        return Jwts.builder()
                .claim("userId", userDetails.getId())
                .claim("username", userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    @Override
    public UserDetails parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }

        try {
            Claims claims = parseClaimsJws(token);
            Long userId = claims.get("userId", Long.class);

            UserDetails userDetails = userDetailService.loadUserById(userId);
            if (userDetails == null) {
                return null;
            }

            userDetails.setTokenId(token);
            userDetails.setLoginTime(claims.getIssuedAt().getTime());
            userDetails.setExpireTime(claims.getExpiration().getTime());

            return userDetails;
        } catch (Exception e) {
            log.error("Failed to parse user from token: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        try {
            parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("JWT token expired: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaimsJws(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaimsJws(token);
        return claims.get("userId", Long.class);
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseClaimsJws(token);
        return claims.get("username", String.class);
    }

    public Long getExpirationFromToken(String token) {
        Claims claims = parseClaimsJws(token);
        return claims.getExpiration().getTime();
    }

    public boolean isTokenExpired(String token) {
        try {
            return getExpirationFromToken(token) < System.currentTimeMillis();
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            log.error("Failed to check token expiration: {}", e.getMessage());
            return true;
        }
    }

    public String refreshToken(String token) {
        UserDetails userDetails = parseToken(token);
        if (userDetails == null) {
            throw new IllegalArgumentException("Invalid token for refresh");
        }
        return generateToken(userDetails);
    }

    public long getTokenRemainingTime(String token) {
        try {
            long expiration = getExpirationFromToken(token);
            long remaining = expiration - System.currentTimeMillis();
            return Math.max(0, remaining);
        } catch (Exception e) {
            return 0;
        }
    }

    public Long getIssuedAtFromToken(String token) {
        Claims claims = parseClaimsJws(token);
        return claims.getIssuedAt().getTime();
    }

    public boolean isTokenValid(String token) {
        return validateToken(token);
    }
}