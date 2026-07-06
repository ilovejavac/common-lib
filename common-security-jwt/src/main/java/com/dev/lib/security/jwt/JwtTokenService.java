package com.dev.lib.security.jwt;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.security.TokenException;
import com.dev.lib.security.service.AuthenticateService;
import com.dev.lib.security.service.TokenService;
import com.dev.lib.security.util.UserDetails;
import com.dev.lib.util.ImmutablePair;
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

@Service
@RequiredArgsConstructor
public class JwtTokenService implements TokenService, InitializingBean {

	private static final String TOKEN_TYPE_CLAIM = "token_type";

	private static final String ACCESS_TOKEN_TYPE = "access";

	private static final String REFRESH_TOKEN_TYPE = "refresh";

	private static final long DEFAULT_ACCESS_EXPIRATION = 86400000L;

	private static final long DEFAULT_REFRESH_EXPIRATION = 1296000000L;

	private final JwtSecurityProperties properties;

	private SecretKey secretKey;

	private final AuthenticateService authenticateService;

	@Override
	public void afterPropertiesSet() throws Exception {

		String secret = properties.getSecret();
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("app.security.jwt.secret must be configured when using JWT security");
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
	public ImmutablePair<String, String> generateToken(UserDetails userDetails) {

		return ImmutablePair.of(
				generateToken(userDetails, ACCESS_TOKEN_TYPE, accessExpiration()),
				generateToken(userDetails, REFRESH_TOKEN_TYPE, refreshExpiration())
		);
	}

	@Override
	public ImmutablePair<String, String> refreshAccessToken(String refreshToken) {

		return generateToken(parseRefreshToken(refreshToken));
	}

	private String generateToken(UserDetails userDetails, String tokenType, long expireMs) {

		long now = System.currentTimeMillis();

		return Jwts.builder()
				.subject(String.valueOf(userDetails.getId()))
				.claim(TOKEN_TYPE_CLAIM, tokenType)
				.claim("sign", "sk-" + IDWorker.newId())
				.issuedAt(new Date(now))
				.expiration(new Date(now + expireMs))
				.signWith(secretKey)
				.compact();
	}

	@Override
	public UserDetails parseToken(String token) {

		try {
			Claims claims = parseClaims(token);
			String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
			if (tokenType != null && !ACCESS_TOKEN_TYPE.equals(tokenType)) {
				throw new TokenException("token无效");
			}

			return loadUser(claims);
		} catch (ExpiredJwtException e) {
			throw new TokenException("token已过期");
		} catch (JwtException e) {
			throw new TokenException("token无效");
		} catch (IllegalArgumentException e) {
			throw new TokenException("token无效");
		}
	}

	private UserDetails parseRefreshToken(String token) {

		try {
			Claims claims = parseClaims(token);
			String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
			if (!REFRESH_TOKEN_TYPE.equals(tokenType)) {
				throw new TokenException("refresh token无效");
			}

			return loadUser(claims);
		} catch (ExpiredJwtException e) {
			throw new TokenException("refresh token已过期");
		} catch (JwtException e) {
			throw new TokenException("refresh token无效");
		} catch (IllegalArgumentException e) {
			throw new TokenException("refresh token无效");
		}
	}

	private Claims parseClaims(String token) {

		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private UserDetails loadUser(Claims claims) {

		Long userid = Long.valueOf(claims.getSubject());
		return authenticateService.loadUserById(userid);
	}

	private long accessExpiration() {

		return properties.getExpiration() != null ? properties.getExpiration() : DEFAULT_ACCESS_EXPIRATION;
	}

	private long refreshExpiration() {

		return properties.getRefreshExpiration() != null ? properties.getRefreshExpiration() : DEFAULT_REFRESH_EXPIRATION;
	}

}
