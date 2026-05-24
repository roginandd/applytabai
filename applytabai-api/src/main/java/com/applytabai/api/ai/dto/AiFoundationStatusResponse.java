package com.applytabai.api.ai.dto;

import java.util.List;

public record AiFoundationStatusResponse(
		String chatProvider,
		boolean chatModelReady,
		List<String> documentReaders
) {
}
