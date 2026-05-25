package com.applytabai.api.resume.infrastructure;

import java.util.UUID;

import com.applytabai.api.resume.dto.StoredResumeFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeFileStorage {

	StoredResumeFile store(UUID userId, MultipartFile file);

	Resource load(String storagePath);

	void delete(String storagePath);
}
