package com.example.Nexpulse.service;

public class EmailAlreadyUsedException extends RuntimeException {

	public EmailAlreadyUsedException() {
		super("An account with that email already exists");
	}
}
