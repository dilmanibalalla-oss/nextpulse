package com.example.Nexpulse.service;

import java.util.Locale;

import com.example.Nexpulse.domain.AppUser;
import com.example.Nexpulse.dto.AuthTokenResponse;
import com.example.Nexpulse.dto.UserLoginRequest;
import com.example.Nexpulse.dto.UserRegistrationRequest;
import com.example.Nexpulse.dto.UserResponse;
import com.example.Nexpulse.repository.AppUserRepository;
import com.example.Nexpulse.security.JwtService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final AppUserRepository users;

	private final PasswordEncoder passwordEncoder;

	private final JwtService jwtService;

	private final String dummyPasswordHash;

	public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.dummyPasswordHash = passwordEncoder.encode("nexpulse-credential-check");
	}

	@Transactional
	public UserResponse register(UserRegistrationRequest request) {
		String email = normalizeEmail(request.email());
		if (users.findByEmail(email).isPresent()) {
			throw new EmailAlreadyUsedException();
		}
		AppUser user = new AppUser(request.name().trim(), email, passwordEncoder.encode(request.password()));
		try {
			users.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException ex) {
			throw new EmailAlreadyUsedException();
		}
		return toResponse(user);
	}

	@Transactional(readOnly = true)
	public AuthTokenResponse login(UserLoginRequest request) {
		String email = normalizeEmail(request.email());
		AppUser user = users.findByEmail(email).orElse(null);
		if (user == null) {
			passwordEncoder.matches(request.password(), dummyPasswordHash);
			throw new InvalidCredentialsException();
		}
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}
		JwtService.IssuedToken token = jwtService.generate(user.getId());
		return new AuthTokenResponse(token.value(), token.expiresAt(), toResponse(user));
	}

	private static UserResponse toResponse(AppUser user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
