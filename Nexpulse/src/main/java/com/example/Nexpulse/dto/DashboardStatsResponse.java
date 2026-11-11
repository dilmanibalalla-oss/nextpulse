package com.example.Nexpulse.dto;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * Counts of the monitors owned by the current user.
 * Status counts include every owned monitor. {@code enabledMonitors} counts those currently checked.
 */
public record DashboardStatsResponse(
		@PositiveOrZero long totalMonitors,
		@PositiveOrZero long enabledMonitors,
		@PositiveOrZero long upMonitors,
		@PositiveOrZero long downMonitors,
		@PositiveOrZero long unknownMonitors
) {
}
