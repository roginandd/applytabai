package com.applytabai.api.common.validation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Common guard methods used by domain entities to protect their invariants.
 */
public final class DomainGuard {

	private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

	private DomainGuard() {
	}

	public static <T> T requireNonNull(T value, String fieldName) {
		return Objects.requireNonNull(value, fieldName + " is required");
	}

	public static String requireNotBlank(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
		return value.trim();
	}

	public static String normalizeEmail(String value) {
		String normalized = requireNotBlank(value, "email").toLowerCase(Locale.ROOT);
		if (!normalized.matches(EMAIL_PATTERN)) {
			throw new IllegalArgumentException("email must be valid");
		}
		return normalized;
	}

	public static String normalizeOptional(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	public static String requireHttpUrl(String value, String fieldName) {
		String normalized = requireNotBlank(value, fieldName);
		try {
			URI uri = new URI(normalized);
			String scheme = uri.getScheme();
			if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
				throw new IllegalArgumentException(fieldName + " must be an HTTP or HTTPS URL");
			}
			if (uri.getHost() == null || uri.getHost().isBlank()) {
				throw new IllegalArgumentException(fieldName + " must include a host");
			}
			return normalized;
		} catch (URISyntaxException exception) {
			throw new IllegalArgumentException(fieldName + " must be a valid URL", exception);
		}
	}

	public static long requirePositive(long value, String fieldName) {
		if (value <= 0) {
			throw new IllegalArgumentException(fieldName + " must be greater than zero");
		}
		return value;
	}

	public static int requireRange(int value, int min, int max, String fieldName) {
		if (value < min || value > max) {
			throw new IllegalArgumentException(fieldName + " must be between " + min + " and " + max);
		}
		return value;
	}

	public static List<String> copyStringList(Collection<String> values, String fieldName) {
		requireNonNull(values, fieldName);
		List<String> normalized = new ArrayList<>(values.size());
		for (String value : values) {
			normalized.add(requireNotBlank(value, fieldName + " item"));
		}
		return List.copyOf(normalized);
	}

	public static <E extends Enum<E>> List<E> copyEnumList(Collection<E> values, String fieldName) {
		requireNonNull(values, fieldName);
		List<E> normalized = new ArrayList<>(values.size());
		for (E value : values) {
			normalized.add(requireNonNull(value, fieldName + " item"));
		}
		return List.copyOf(normalized);
	}
}
