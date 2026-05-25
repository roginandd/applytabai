package com.applytabai.api.common.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.applytabai.api.auth.config.SecurityConfiguration;
import com.applytabai.api.common.service.impl.StatusServiceImpl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatusController.class)
@Import({SecurityConfiguration.class, StatusServiceImpl.class})
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
class StatusControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsPhaseOneStatus() throws Exception {
		mockMvc.perform(get("/api/v1/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.service").value("applytabai-api"))
				.andExpect(jsonPath("$.data.phase").value("phase1"))
				.andExpect(jsonPath("$.data.status").value("UP"));
	}

	@Test
	void rejectsProtectedApiWithoutBearerToken() throws Exception {
		mockMvc.perform(get("/api/v1/jobs"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));
	}
}
