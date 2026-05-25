package com.applytabai.api.resume.controller;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import com.applytabai.api.auth.config.SecurityConfiguration;
import com.applytabai.api.resume.domain.ResumeFile;
import com.applytabai.api.resume.dto.ResumeFileDownload;
import com.applytabai.api.resume.dto.ResumeFileResponse;
import com.applytabai.api.resume.service.ResumeFileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResumeFileController.class)
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
class ResumeFileControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ResumeFileService resumeFileService;

	@Test
	void uploadReturnsCreatedResumeMetadata() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID resumeFileId = UUID.randomUUID();
		when(resumeFileService.upload(eq(userId.toString()), any(), eq(true))).thenReturn(response(resumeFileId, true));

		mockMvc.perform(multipart("/api/v1/resume-files")
						.file(pdf())
						.param("defaultResume", "true")
						.with(jwt().jwt(jwt -> jwt
								.subject(userId.toString())
								.claim("email", "jane@example.com")
								.claim("role", "USER")
						)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(resumeFileId.toString()))
				.andExpect(jsonPath("$.data.originalFilename").value("resume.pdf"))
				.andExpect(jsonPath("$.data.defaultResume").value(true));

		verify(resumeFileService).upload(eq(userId.toString()), any(), eq(true));
	}

	@Test
	void uploadRequiresAuthentication() throws Exception {
		mockMvc.perform(multipart("/api/v1/resume-files").file(pdf()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));
	}

	@Test
	void downloadReturnsPdfBytes() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID resumeFileId = UUID.randomUUID();
		byte[] bytes = "%PDF- download".getBytes(StandardCharsets.UTF_8);
		when(resumeFileService.download(userId.toString(), resumeFileId)).thenReturn(new ResumeFileDownload(
				"resume.pdf",
				ResumeFile.PDF_CONTENT_TYPE,
				bytes.length,
				new ByteArrayResource(bytes)
		));

		mockMvc.perform(get("/api/v1/resume-files/{resumeFileId}/download", resumeFileId)
						.with(jwt().jwt(jwt -> jwt
								.subject(userId.toString())
								.claim("email", "jane@example.com")
								.claim("role", "USER")
						)))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("resume.pdf")))
				.andExpect(content().contentType(ResumeFile.PDF_CONTENT_TYPE))
				.andExpect(content().bytes(bytes));
	}

	@Test
	void deleteReturnsSuccessEnvelope() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID resumeFileId = UUID.randomUUID();

		mockMvc.perform(delete("/api/v1/resume-files/{resumeFileId}", resumeFileId)
						.with(jwt().jwt(jwt -> jwt
								.subject(userId.toString())
								.claim("email", "jane@example.com")
								.claim("role", "USER")
						)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		verify(resumeFileService).delete(userId.toString(), resumeFileId);
	}

	private MockMultipartFile pdf() {
		return new MockMultipartFile(
				"file",
				"resume.pdf",
				MediaType.APPLICATION_PDF_VALUE,
				"%PDF- test resume".getBytes(StandardCharsets.UTF_8)
		);
	}

	private ResumeFileResponse response(UUID resumeFileId, boolean defaultResume) {
		return new ResumeFileResponse(
				resumeFileId,
				"resume.pdf",
				ResumeFile.PDF_CONTENT_TYPE,
				1000,
				defaultResume,
				Instant.parse("2026-05-25T12:00:00Z"),
				Instant.parse("2026-05-25T12:00:00Z")
		);
	}
}
