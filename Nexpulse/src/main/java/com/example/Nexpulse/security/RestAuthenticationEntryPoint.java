package com.example.Nexpulse.security;

import java.io.IOException;

import com.example.Nexpulse.web.ApiErrorWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ApiErrorWriter errors;

	public RestAuthenticationEntryPoint(ApiErrorWriter errors) {
		this.errors = errors;
	}

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authException
	) throws IOException {
		String message = authException instanceof InvalidTokenException
				? authException.getMessage()
				: "Authentication is required";
		response.setHeader("WWW-Authenticate", "Bearer realm=\"nexpulse\"");
		errors.write(response, HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), message);
	}
}
