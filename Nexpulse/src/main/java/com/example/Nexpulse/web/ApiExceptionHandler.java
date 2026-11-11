package com.example.Nexpulse.web;

import java.util.LinkedHashMap;
import java.util.Map;

import com.example.Nexpulse.security.InvalidTokenException;
import com.example.Nexpulse.service.EmailAlreadyUsedException;
import com.example.Nexpulse.service.InvalidCredentialsException;
import com.example.Nexpulse.service.MonitorNotFoundException;

import org.springframework.security.core.AuthenticationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
		Map<String, String> fields = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fields.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		return error(HttpStatus.BAD_REQUEST, "Validation failed", fields);
	}

	@ExceptionHandler({
			HandlerMethodValidationException.class,
			HttpMessageNotReadableException.class
	})
	ResponseEntity<ApiError> badRequest() {
		return error(HttpStatus.BAD_REQUEST, "Invalid request", null);
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	ResponseEntity<ApiError> invalidCredentials(InvalidCredentialsException ex) {
		return error(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ApiError> authentication(AuthenticationException ex) {
		String message = ex instanceof InvalidTokenException
				? ex.getMessage()
				: "Authentication is required";
		return error(HttpStatus.UNAUTHORIZED, message, null);
	}

	@ExceptionHandler(EmailAlreadyUsedException.class)
	ResponseEntity<ApiError> emailAlreadyUsed(EmailAlreadyUsedException ex) {
		return error(HttpStatus.CONFLICT, ex.getMessage(), null);
	}

	@ExceptionHandler(MonitorNotFoundException.class)
	ResponseEntity<ApiError> monitorNotFound(MonitorNotFoundException ex) {
		return error(HttpStatus.NOT_FOUND, ex.getMessage(), null);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> unexpected(Exception ex) {
		log.error("Request failed: {}", ex.getClass().getSimpleName());
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", null);
	}

	private static ResponseEntity<ApiError> error(HttpStatus status, String message, Map<String, String> fields) {
		ApiError body = fields == null
				? ApiError.of(status.value(), status.getReasonPhrase(), message)
				: ApiError.of(status.value(), status.getReasonPhrase(), message, fields);
		return ResponseEntity.status(status).body(body);
	}
}
