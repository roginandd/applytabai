package com.applytabai.api.resume.dto;

import java.time.Instant;
import java.util.UUID;

import com.applytabai.api.resume.domain.ResumeFile;

public record ResumeFileResponse(
		UUID id,
		String originalFilename,
		String contentType,
		long fileSize,
		boolean defaultResume,
		Instant createdAt,
		Instant updatedAt
) {

	public static ResumeFileResponse from(ResumeFile resumeFile) {
		return new ResumeFileResponse(
				resumeFile.getId(),
				resumeFile.getOriginalFilename(),
				resumeFile.getContentType(),
				resumeFile.getFileSize(),
				resumeFile.isDefaultResume(),
				resumeFile.getCreatedAt(),
				resumeFile.getUpdatedAt()
		);
	}
}
