package com.applytabai.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@Email
		@NotBlank
		@Size(max = 255)
		String email,

		@NotBlank
		@Size(max = 255)
		String fullName,

		@NotBlank
		@Size(min = 8, max = 128)
		String password,

		@NotBlank
		@Size(min = 8, max = 128)
		String confirmPassword
) {
}
