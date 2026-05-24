package com.applytabai.api.resume.repository;

import java.util.List;
import java.util.UUID;

import com.applytabai.api.resume.domain.ResumeFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeFileRepository extends JpaRepository<ResumeFile, UUID> {

	List<ResumeFile> findByUser_Id(UUID userId);
}
