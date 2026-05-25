package com.applytabai.api.auth.service;

import com.applytabai.api.auth.dto.AuthTokenResponse;
import com.applytabai.api.auth.dto.LoginRequest;
import com.applytabai.api.auth.dto.LogoutRequest;
import com.applytabai.api.auth.dto.RefreshTokenRequest;
import com.applytabai.api.auth.dto.RegisterRequest;
import com.applytabai.api.auth.dto.UserSummaryResponse;

public interface AuthService {

	AuthTokenResponse register(RegisterRequest request);

	AuthTokenResponse login(LoginRequest request);

	AuthTokenResponse refresh(RefreshTokenRequest request);

	void logout(LogoutRequest request);

	UserSummaryResponse currentUser(String subject);
}
