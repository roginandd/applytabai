package com.applytabai.api.auth.dto;

import java.util.UUID;

import com.applytabai.api.users.domain.UserAccount;

public record UserSummaryResponse(
		UUID id,
		String email,
		String fullName,
		String role
) {

	public static UserSummaryResponse from(UserAccount user) {
		return new UserSummaryResponse(
				user.getId(),
				user.getEmail(),
				user.getFullName(),
				user.getRole().name()
		);
	}
}
