package com.applytabai.api.resume.validation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.applytabai.api.common.exception.ApiException;
import com.applytabai.api.common.exception.ErrorCode;
import com.applytabai.api.resume.domain.ResumeFile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ResumeFileValidator {

	private static final byte[] PDF_HEADER = "%PDF-".getBytes(StandardCharsets.US_ASCII);

	public void validatePdf(MultipartFile file) {
		if (file == null || file.isEmpty() || file.getSize() <= 0) {
			throw validationFailure("Resume file is required");
		}

		String originalFilename = StringUtils.getFilename(file.getOriginalFilename());
		if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
			throw validationFailure("Resume file must use a .pdf filename");
		}

		String contentType = file.getContentType();
		if (!ResumeFile.PDF_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
			throw validationFailure("Resume file content type must be application/pdf");
		}

		if (!hasPdfHeader(file)) {
			throw validationFailure("Resume file must be a valid PDF");
		}
	}

	public String normalizedOriginalFilename(MultipartFile file) {
		String filename = StringUtils.getFilename(file.getOriginalFilename());
		if (filename == null || filename.isBlank()) {
			return "resume.pdf";
		}
		return filename.trim();
	}

	private boolean hasPdfHeader(MultipartFile file) {
		try (InputStream inputStream = file.getInputStream()) {
			for (byte expected : PDF_HEADER) {
				if (inputStream.read() != Byte.toUnsignedInt(expected)) {
					return false;
				}
			}
			return true;
		} catch (IOException exception) {
			throw new ApiException(
					ErrorCode.VALIDATION_FAILED,
					HttpStatus.BAD_REQUEST,
					"Could not read resume file"
			);
		}
	}

	private ApiException validationFailure(String message) {
		return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
	}
}
