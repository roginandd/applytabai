package com.applytabai.api.resume.service;

import java.util.List;
import java.util.UUID;

import com.applytabai.api.resume.dto.ResumeFileDownload;
import com.applytabai.api.resume.dto.ResumeFileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeFileService {

	ResumeFileResponse upload(String subject, MultipartFile file, boolean defaultResume);

	List<ResumeFileResponse> list(String subject);

	ResumeFileResponse getDefault(String subject);

	ResumeFileDownload download(String subject, UUID resumeFileId);

	ResumeFileResponse markDefault(String subject, UUID resumeFileId);

	void delete(String subject, UUID resumeFileId);
}
