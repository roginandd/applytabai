package com.applytabai.api.resume.dto;

public record StoredResumeFile(
		String storedFilename,
		String storagePath,
		String contentType,
		long fileSize
) {
}
