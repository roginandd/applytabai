package com.applytabai.api.applications.repository;

import java.util.List;
import java.util.UUID;

import com.applytabai.api.applications.domain.ApplicationStatus;
import com.applytabai.api.applications.domain.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

	List<JobApplication> findByUser_IdAndStatus(UUID userId, ApplicationStatus status);
}
