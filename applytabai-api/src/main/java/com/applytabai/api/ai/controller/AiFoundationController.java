package com.applytabai.api.ai.controller;

import com.applytabai.api.ai.dto.AiFoundationStatusResponse;
import com.applytabai.api.ai.service.AiFoundationService;
import com.applytabai.api.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/foundation")
public class AiFoundationController {

	private final AiFoundationService aiFoundationService;

	public AiFoundationController(AiFoundationService aiFoundationService) {
		this.aiFoundationService = aiFoundationService;
	}

	@GetMapping
	public ApiResponse<AiFoundationStatusResponse> status() {
		return ApiResponse.success(aiFoundationService.getStatus());
	}
}
