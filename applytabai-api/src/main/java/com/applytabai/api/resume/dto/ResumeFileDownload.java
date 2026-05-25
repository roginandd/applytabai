package com.applytabai.api.resume.dto;

import org.springframework.core.io.Resource;

public record ResumeFileDownload(
		String originalFilename,
		String contentType,
		long contentLength,
		Resource resource
) {
}
