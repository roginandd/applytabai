package com.applytabai.api.applications.domain;

/**
 * Current pipeline state for a tracked job application.
 */
public enum ApplicationStatus {
	SAVED,
	READY_TO_APPLY,
	APPLIED,
	INTERVIEW,
	REJECTED,
	OFFER,
	ARCHIVED
}
