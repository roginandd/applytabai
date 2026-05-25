package com.applytabai.api.resume.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.applytabai.api.common.response.ApiResponse;
import com.applytabai.api.resume.dto.ResumeFileDownload;
import com.applytabai.api.resume.dto.ResumeFileResponse;
import com.applytabai.api.resume.service.ResumeFileService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/resume-files")
public class ResumeFileController {

	private final ResumeFileService resumeFileService;

	public ResumeFileController(ResumeFileService resumeFileService) {
		this.resumeFileService = resumeFileService;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<ResumeFileResponse>> upload(
			@AuthenticationPrincipal Jwt jwt,
			@RequestPart("file") MultipartFile file,
			@RequestParam(defaultValue = "false") boolean defaultResume
	) {
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(ApiResponse.success(resumeFileService.upload(jwt.getSubject(), file, defaultResume)));
	}

	@GetMapping
	public ApiResponse<List<ResumeFileResponse>> list(@AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.success(resumeFileService.list(jwt.getSubject()));
	}

	@GetMapping("/default")
	public ApiResponse<ResumeFileResponse> getDefault(@AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.success(resumeFileService.getDefault(jwt.getSubject()));
	}

	@PatchMapping("/{resumeFileId}/default")
	public ApiResponse<ResumeFileResponse> markDefault(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID resumeFileId
	) {
		return ApiResponse.success(resumeFileService.markDefault(jwt.getSubject(), resumeFileId));
	}

	@GetMapping("/{resumeFileId}/download")
	public ResponseEntity<Resource> download(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID resumeFileId
	) {
		ResumeFileDownload download = resumeFileService.download(jwt.getSubject(), resumeFileId);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(download.contentType()))
				.contentLength(download.contentLength())
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(download.originalFilename(), StandardCharsets.UTF_8)
						.build()
						.toString())
				.body(download.resource());
	}

	@DeleteMapping("/{resumeFileId}")
	public ApiResponse<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID resumeFileId) {
		resumeFileService.delete(jwt.getSubject(), resumeFileId);
		return ApiResponse.success(null);
	}
}
