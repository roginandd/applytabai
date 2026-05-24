package com.applytabai.api.preferences.repository;

import java.util.Optional;
import java.util.UUID;

import com.applytabai.api.preferences.domain.JobPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPreferenceRepository extends JpaRepository<JobPreference, UUID> {

	Optional<JobPreference> findByUser_Id(UUID userId);
}
