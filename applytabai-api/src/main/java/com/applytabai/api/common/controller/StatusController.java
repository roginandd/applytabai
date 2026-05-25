package com.applytabai.api.common.controller;

import com.applytabai.api.common.dto.StatusResponse;
import com.applytabai.api.common.response.ApiResponse;
import com.applytabai.api.common.service.StatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/status")
public class StatusController {

	private final StatusService statusService;

	public StatusController(StatusService statusService) {
		this.statusService = statusService;
	}

	@GetMapping
	public ApiResponse<StatusResponse> status() {
		return ApiResponse.success(statusService.status());
	}
}
