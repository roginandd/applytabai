package com.applytabai.api.jobs.repository;

import java.util.Optional;
import java.util.UUID;

import com.applytabai.api.jobs.domain.JobAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobAnalysisRepository extends JpaRepository<JobAnalysis, UUID> {

	Optional<JobAnalysis> findByJobPost_Id(UUID jobPostId);
}
