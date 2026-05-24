package com.applytabai.api.common.controller;

import com.applytabai.api.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/status")
public class StatusController {

	@GetMapping
	public ApiResponse<StatusResponse> status() {
		return ApiResponse.success(new StatusResponse("applytabai-api", "phase1", "UP"));
	}

	public record StatusResponse(String service, String phase, String status) {
	}
}
