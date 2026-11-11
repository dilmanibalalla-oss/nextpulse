package com.example.Nexpulse.security;

import java.io.IOException;
import java.util.List;

import com.example.Nexpulse.domain.AppUser;
import com.example.Nexpulse.repository.AppUserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	static final int MAX_TOKEN_LENGTH = 4096;

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	private final AppUserRepository users;

	private final RestAuthenticationEntryPoint authenticationEntryPoint;

	public JwtAuthenticationFilter(
			JwtService jwtService,
			AppUserRepository users,
			RestAuthenticationEntryPoint authenticationEntryPoint
	) {
		this.jwtService = jwtService;
		this.users = users;
		this.authenticationEntryPoint = authenticationEntryPoint;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null) {
			filterChain.doFilter(request, response);
			return;
		}
		if (!header.startsWith(BEARER_PREFIX)) {
			reject(request, response);
			return;
		}
		String token = header.substring(BEARER_PREFIX.length()).trim();
		if (token.isEmpty() || token.length() > MAX_TOKEN_LENGTH) {
			reject(request, response);
			return;
		}
		try {
			long userId = jwtService.userId(token);
			AppUser user = users.findById(userId).orElseThrow(InvalidTokenException::new);
			AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail());
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					principal,
					null,
					List.of(new SimpleGrantedAuthority(AuthenticatedUser.ROLE_USER))
			);
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, response);
		}
		catch (InvalidTokenException ex) {
			SecurityContextHolder.clearContext();
			reject(request, response);
		}
	}

	private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
		SecurityContextHolder.clearContext();
		authenticationEntryPoint.commence(request, response, new InvalidTokenException());
	}
}
