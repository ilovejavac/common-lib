package com.dev.lib.security.jwt;

import com.dev.lib.security.TokenException;
import com.dev.lib.security.model.EndpointPermission;
import com.dev.lib.security.service.AuthenticateService;
import com.dev.lib.security.util.UserDetails;
import com.dev.lib.util.ImmutablePair;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

	private static final String SECRET = "jwt-secret-value-with-enough-length";

	private static final long FIFTEEN_DAYS_MS = 1_296_000_000L;

	@Test
	void shouldRejectMissingJwtSecret() {

		JwtSecurityProperties properties = new JwtSecurityProperties();
		JwtTokenService service = new JwtTokenService(
				properties,
				null
		);

		assertThatThrownBy(service::afterPropertiesSet)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("app.security.jwt.secret must be configured when using JWT security");
	}

	@Test
	void shouldGenerateAccessAndRefreshTokenTogether() throws Exception {

		JwtTokenService service = tokenService(60_000L, 120_000L);
		UserDetails user = UserDetails.builder()
				.id(7L)
				.username("demo")
				.validated(true)
				.build();

		ImmutablePair<String, String> tokenPair = service.generateToken(user);
		String accessToken = tokenPair.key();
		String refreshToken = tokenPair.value();
		ImmutablePair<String, String> refreshedTokenPair = service.refreshAccessToken(refreshToken);
		String refreshedAccessToken = refreshedTokenPair.key();
		String refreshedRefreshToken = refreshedTokenPair.value();

		assertThat(accessToken).isNotBlank();
		assertThat(accessToken).isNotEqualTo(refreshToken);
		assertThat(service.parseToken(accessToken).getId()).isEqualTo(7L);
		assertThat(refreshedAccessToken).isNotBlank();
		assertThat(refreshedRefreshToken).isNotBlank();
		assertThat(refreshedAccessToken).isNotEqualTo(refreshedRefreshToken);
		assertThat(service.parseToken(refreshedAccessToken).getId()).isEqualTo(7L);
		assertThat(service.refreshAccessToken(refreshedRefreshToken).key()).isNotBlank();
	}

	@Test
	void shouldRejectAccessTokenWhenRefreshingAccessToken() throws Exception {

		JwtTokenService service = tokenService(60_000L, 120_000L);
		String accessToken = service.generateToken(UserDetails.builder().id(7L).build()).key();

		assertThatThrownBy(() -> service.refreshAccessToken(accessToken))
				.isInstanceOf(TokenException.class)
				.hasMessage("refresh token无效");
	}

	@Test
	void shouldRejectRefreshTokenWhenParsingAccessToken() throws Exception {

		JwtTokenService service = tokenService(60_000L, 120_000L);
		String refreshToken = service.generateToken(UserDetails.builder().id(7L).build()).value();

		assertThatThrownBy(() -> service.parseToken(refreshToken))
				.isInstanceOf(TokenException.class)
				.hasMessage("token无效");
	}

	@Test
	void shouldRejectExpiredRefreshTokenWhenRefreshingAccessToken() throws Exception {

		JwtTokenService service = tokenService(60_000L, -1L);
		String refreshToken = service.generateToken(UserDetails.builder().id(7L).build()).value();

		assertThatThrownBy(() -> service.refreshAccessToken(refreshToken))
				.isInstanceOf(TokenException.class)
				.hasMessage("refresh token已过期");
	}

	@Test
	void shouldUseFifteenDaysAsDefaultRefreshExpiration() throws Exception {

		JwtSecurityProperties properties = new JwtSecurityProperties();
		properties.setSecret(SECRET);
		properties.setExpiration(60_000L);
		JwtTokenService service = new JwtTokenService(
				properties,
				new StubAuthenticateService()
		);
		service.afterPropertiesSet();

		String refreshToken = service.generateToken(UserDetails.builder().id(7L).build()).value();
		Claims claims = Jwts.parser()
				.verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
				.build()
				.parseSignedClaims(refreshToken)
				.getPayload();

		assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(FIFTEEN_DAYS_MS);
	}

	private JwtTokenService tokenService(long expiration, long refreshExpiration) throws Exception {

		JwtSecurityProperties properties = new JwtSecurityProperties();
		properties.setSecret(SECRET);
		properties.setExpiration(expiration);
		properties.setRefreshExpiration(refreshExpiration);
		JwtTokenService service = new JwtTokenService(
				properties,
				new StubAuthenticateService()
		);
		service.afterPropertiesSet();
		return service;
	}

	private static class StubAuthenticateService implements AuthenticateService {

		@Override
		public UserDetails loadUserById(Long id) {

			return UserDetails.builder()
					.id(id)
					.username("user-" + id)
					.validated(true)
					.build();
		}

		@Override
		public Collection<UserDetails> batchLoadUserByIds(Set<Long> ids) {

			return List.of();
		}

		@Override
		public void registerPermissions(List<EndpointPermission> permissions) {

		}
	}
}
