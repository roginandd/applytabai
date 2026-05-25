package com.applytabai.api.auth.controller;

import java.time.Instant;
import java.util.UUID;

import com.applytabai.api.auth.config.SecurityConfiguration;
import com.applytabai.api.auth.dto.AuthTokenResponse;
import com.applytabai.api.auth.dto.UserSummaryResponse;
import com.applytabai.api.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfiguration.class)
@ImportAutoConfiguration({
		SecurityAutoConfiguration.class,
		ServletWebSecurityAutoConfiguration.class,
		OAuth2ResourceServerAutoConfiguration.class
})
@TestPropertySource(properties = {
		"app.security.jwt.secret=test-only-applytabai-jwt-secret-change-me-32-chars-minimum",
		"app.security.jwt.issuer=applytabai-api",
		"app.security.jwt.access-token-ttl=PT15M",
		"app.security.jwt.refresh-token-ttl=P7D",
		"app.security.cors.allowed-origins=http://localhost:5173,http://127.0.0.1:5173"
})
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@Test
	void registerReturnsCreatedTokenPair() throws Exception {
		when(authService.register(any())).thenReturn(tokenResponse());

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "jane@example.com",
								  "fullName": "Jane Developer",
								  "password": "password123",
								  "confirmPassword": "password123"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.data.accessToken").value("access-token"))
				.andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
				.andExpect(jsonPath("$.data.user.email").value("jane@example.com"));
	}

	@Test
	void rejectsInvalidRegistrationRequest() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "not-an-email",
								  "fullName": "Jane Developer",
								  "password": "short",
								  "confirmPassword": "short"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void loginReturnsTokenPair() throws Exception {
		when(authService.login(any())).thenReturn(tokenResponse());

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "jane@example.com",
								  "password": "password123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.accessToken").value("access-token"))
				.andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
	}

	@Test
	void refreshReturnsRotatedTokenPair() throws Exception {
		when(authService.refresh(any())).thenReturn(tokenResponse());

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "refreshToken": "refresh-token"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.accessToken").value("access-token"))
				.andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
	}

	@Test
	void meRequiresBearerToken() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));
	}

	@Test
	void meReturnsAuthenticatedUser() throws Exception {
		UUID userId = UUID.randomUUID();
		when(authService.currentUser(userId.toString())).thenReturn(new UserSummaryResponse(
				userId,
				"jane@example.com",
				"Jane Developer",
				"USER"
		));

		mockMvc.perform(get("/api/v1/auth/me")
						.with(jwt().jwt(jwt -> jwt
								.subject(userId.toString())
								.claim("email", "jane@example.com")
								.claim("role", "USER")
						)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(userId.toString()))
				.andExpect(jsonPath("$.data.email").value("jane@example.com"));

		verify(authService).currentUser(userId.toString());
	}

	private AuthTokenResponse tokenResponse() {
		return new AuthTokenResponse(
				"Bearer",
				"access-token",
				Instant.parse("2026-05-24T12:15:00Z"),
				"refresh-token",
				Instant.parse("2026-05-31T12:00:00Z"),
				new UserSummaryResponse(UUID.randomUUID(), "jane@example.com", "Jane Developer", "USER")
		);
	}
}
