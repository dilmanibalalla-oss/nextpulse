package com.example.Nexpulse.web;

import java.util.List;

import com.example.Nexpulse.dto.CheckResultResponse;
import com.example.Nexpulse.dto.CreateMonitorRequest;
import com.example.Nexpulse.dto.MonitorResponse;
import com.example.Nexpulse.dto.UpdateMonitorRequest;
import com.example.Nexpulse.service.MonitorService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitors")
@Validated
public class MonitorController {

	private final MonitorService monitors;

	public MonitorController(MonitorService monitors) {
		this.monitors = monitors;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MonitorResponse create(@Valid @RequestBody CreateMonitorRequest request) {
		return monitors.create(request);
	}

	@GetMapping
	public List<MonitorResponse> list() {
		return monitors.list();
	}

	@GetMapping("/{id}")
	public MonitorResponse get(@PathVariable Long id) {
		return monitors.get(id);
	}

	@PutMapping("/{id}")
	public MonitorResponse update(@PathVariable Long id, @Valid @RequestBody UpdateMonitorRequest request) {
		return monitors.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		monitors.delete(id);
	}

	@GetMapping("/{id}/checks")
	public Page<CheckResultResponse> checks(
			@PathVariable Long id,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return monitors.history(id, page, size);
	}
}
