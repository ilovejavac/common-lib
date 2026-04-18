package com.dev.lib.security.jwt;

import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.security.TokenException;
import com.dev.lib.security.service.AuthenticateService;
import com.dev.lib.security.service.TokenManager;
import com.dev.lib.security.service.TokenService;
import com.dev.lib.security.util.UserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtTokenService implements TokenService, InitializingBean {

    private final AppSecurityProperties properties;

    private final TokenManager tokenManager;

    private SecretKey secretKey;

    private final AuthenticateService authenticateService;

    @Override
    public void afterPropertiesSet() throws Exception {

        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.security.secret must be configured when using JWT security");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            // 不是 Base64，当普通字符串处理
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String generateToken(UserDetails userDetails) {

        long now      = System.currentTimeMillis();
        long expireMs = properties.getExpiration() != null ? properties.getExpiration() : 86400000L;

        userDetails.setLoginTime(now);
        userDetails.setExpireTime(now + expireMs);
        userDetails.setTokenId(UUID.randomUUID().toString());

        return Jwts.builder()
                .subject(String.valueOf(userDetails.getId()))
                .claim("userid", userDetails.getId())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireMs))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public UserDetails parseToken(String token) {

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userid = claims.get("userid", Long.class);
            return authenticateService.loadUserById(userid);
        } catch (ExpiredJwtException e) {
            throw new TokenException("token已过期");
        } catch (JwtException e) {
            throw new TokenException("token无效");
        }
    }

}
