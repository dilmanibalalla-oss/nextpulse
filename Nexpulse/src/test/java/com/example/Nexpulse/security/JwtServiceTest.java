package com.example.Nexpulse.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

import com.example.Nexpulse.config.JwtProperties;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

	private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

	private MutableClock clock;

	private JwtService jwtService;

	@BeforeEach
	void setUp() {
		clock = new MutableClock(Instant.parse("2026-11-11T12:00:00Z"));
		jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(15)), clock);
	}

	@Test
	void issuesATokenThatResolvesToTheUser() {
		JwtService.IssuedToken token = jwtService.generate(4L);

		assertThat(jwtService.userId(token.value())).isEqualTo(4L);
		assertThat(token.expiresAt()).isEqualTo(Instant.parse("2026-11-11T12:15:00Z"));
		assertThat(token.toString()).doesNotContain(token.value());
	}

	@Test
	void rejectsExpiredTamperedUnsignedAndWrongAlgorithmTokens() throws Exception {
		String token = jwtService.generate(4L).value();
		String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

		clock.advance(Duration.ofMinutes(15));
		assertThatThrownBy(() -> jwtService.userId(token)).isInstanceOf(InvalidTokenException.class);

		clock.advance(Duration.ofMinutes(-15));
		assertThatThrownBy(() -> jwtService.userId(tampered)).isInstanceOf(InvalidTokenException.class);
		assertThatThrownBy(() -> jwtService.userId(unsignedToken())).isInstanceOf(InvalidTokenException.class);
		assertThatThrownBy(() -> jwtService.userId(signedWith(JWSAlgorithm.HS384, JwtService.ISSUER)))
				.isInstanceOf(InvalidTokenException.class);
		assertThatThrownBy(() -> jwtService.userId(signedWith(JWSAlgorithm.HS256, "other-app")))
				.isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void secretAndExpirationStayInsideSafeBounds() {
		assertThatThrownBy(() -> new JwtProperties("short-secret", Duration.ofMinutes(15)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("JWT_SECRET must be at least 32 bytes");
		assertThatThrownBy(() -> new JwtProperties(" ", Duration.ofMinutes(15)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("JWT_SECRET is required");
		assertThatThrownBy(() -> new JwtProperties(SECRET, Duration.ofSeconds(30)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("JWT_EXPIRATION must be between 1 minute and 24 hours");
		assertThatThrownBy(() -> new JwtProperties(SECRET, Duration.ofHours(25)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("JWT_EXPIRATION must be between 1 minute and 24 hours");
		assertThat(new JwtProperties(SECRET, Duration.ofMinutes(1)).expiration()).isEqualTo(Duration.ofMinutes(1));
		assertThat(new JwtProperties(SECRET, Duration.ofHours(24)).expiration()).isEqualTo(Duration.ofHours(24));
	}

	private String unsignedToken() throws Exception {
		return new PlainJWT(new JWTClaimsSet.Builder()
				.issuer(JwtService.ISSUER)
				.subject("4")
				.expirationTime(Date.from(clock.instant().plusSeconds(60)))
				.build()).serialize();
	}

	private String signedWith(JWSAlgorithm algorithm, String issuer) throws Exception {
		SignedJWT jwt = new SignedJWT(
				new JWSHeader(algorithm),
				new JWTClaimsSet.Builder()
						.issuer(issuer)
						.subject("4")
						.expirationTime(Date.from(clock.instant().plusSeconds(60)))
						.build()
		);
		jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
		return jwt.serialize();
	}

	private static final class MutableClock extends Clock {

		private Instant instant;

		private MutableClock(Instant instant) {
			this.instant = instant;
		}

		private void advance(Duration duration) {
			instant = instant.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
