package com.applytabai.api.auth.service;

import java.time.Instant;

import com.applytabai.api.users.domain.UserAccount;

public interface JwtTokenService {

	IssuedAccessToken issueAccessToken(UserAccount user);

	record IssuedAccessToken(String value, Instant expiresAt) {
	}
}
