package com.example.Nexpulse.security;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import com.example.Nexpulse.config.JwtProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.springframework.stereotype.Service;

@Service
public class JwtService {

	public static final String ISSUER = "nexpulse";

	private final JwtProperties properties;

	private final Clock clock;

	private final byte[] secret;

	public JwtService(JwtProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		this.secret = properties.secret().getBytes(StandardCharsets.UTF_8);
	}

	public IssuedToken generate(long userId) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plus(properties.expiration());
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.issuer(ISSUER)
				.subject(Long.toString(userId))
				.jwtID(UUID.randomUUID().toString())
				.issueTime(Date.from(issuedAt))
				.expirationTime(Date.from(expiresAt))
				.build();
		try {
			SignedJWT jwt = new SignedJWT(
					new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),
					claims
			);
			jwt.sign(new MACSigner(secret));
			return new IssuedToken(jwt.serialize(), expiresAt);
		}
		catch (JOSEException ex) {
			throw new IllegalStateException("Could not issue a token");
		}
	}

	public long userId(String token) {
		try {
			SignedJWT jwt = SignedJWT.parse(token);
			if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) {
				throw new InvalidTokenException();
			}
			if (!jwt.verify(new MACVerifier(secret))) {
				throw new InvalidTokenException();
			}
			JWTClaimsSet claims = jwt.getJWTClaimsSet();
			if (!ISSUER.equals(claims.getIssuer())) {
				throw new InvalidTokenException();
			}
			Instant now = clock.instant();
			Date expiration = claims.getExpirationTime();
			if (expiration == null || !expiration.toInstant().isAfter(now)) {
				throw new InvalidTokenException();
			}
			Date notBefore = claims.getNotBeforeTime();
			if (notBefore != null && notBefore.toInstant().isAfter(now)) {
				throw new InvalidTokenException();
			}
			String subject = claims.getSubject();
			if (subject == null) {
				throw new InvalidTokenException();
			}
			long userId = Long.parseLong(subject);
			if (userId <= 0) {
				throw new InvalidTokenException();
			}
			return userId;
		}
		catch (InvalidTokenException ex) {
			throw ex;
		}
		catch (JOSEException | ParseException | RuntimeException ex) {
			throw new InvalidTokenException();
		}
	}

	public record IssuedToken(String value, Instant expiresAt) {

		@Override
		public String toString() {
			return "IssuedToken[expiresAt=" + expiresAt + "]";
		}
	}
}
