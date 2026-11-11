package com.example.Nexpulse.service;

public class MonitorNotFoundException extends RuntimeException {

	public MonitorNotFoundException() {
		super("Monitor not found");
	}
}
