package com.example.Nexpulse.domain;

/**
 * Latest known availability of a monitor.
 * A monitor stays {@link #UNKNOWN} until the first check is recorded.
 */
public enum MonitorStatus {
	UNKNOWN,
	UP,
	DOWN
}
