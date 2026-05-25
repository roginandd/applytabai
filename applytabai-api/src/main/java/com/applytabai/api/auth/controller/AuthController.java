package com.applytabai.api.auth.controller;

import com.applytabai.api.auth.dto.AuthTokenResponse;
import com.applytabai.api.auth.dto.LoginRequest;
import com.applytabai.api.auth.dto.LogoutRequest;
import com.applytabai.api.auth.dto.RefreshTokenRequest;
import com.applytabai.api.auth.dto.RegisterRequest;
import com.applytabai.api.auth.dto.UserSummaryResponse;
import com.applytabai.api.auth.service.AuthService;
import com.applytabai.api.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<AuthTokenResponse>> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(ApiResponse.success(authService.register(request)));
	}

	@PostMapping("/login")
	public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.success(authService.login(request));
	}

	@PostMapping("/refresh")
	public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.success(authService.refresh(request));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
		authService.logout(request);
		return ApiResponse.success(null);
	}

	@GetMapping("/me")
	public ApiResponse<UserSummaryResponse> me(@AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.success(authService.currentUser(jwt.getSubject()));
	}
}
