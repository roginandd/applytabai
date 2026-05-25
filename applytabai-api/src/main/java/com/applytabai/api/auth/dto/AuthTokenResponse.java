package com.applytabai.api.auth.dto;

import java.time.Instant;

public record AuthTokenResponse(
		String tokenType,
		String accessToken,
		Instant accessTokenExpiresAt,
		String refreshToken,
		Instant refreshTokenExpiresAt,
		UserSummaryResponse user
) {
}
