package com.applytabai.api.ai.controller;

import java.util.List;

import com.applytabai.api.ai.dto.AiFoundationStatusResponse;
import com.applytabai.api.ai.service.AiFoundationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiFoundationController.class)
class AiFoundationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AiFoundationService aiFoundationService;

	@Test
	void returnsAiFoundationStatus() throws Exception {
		when(aiFoundationService.getStatus()).thenReturn(new AiFoundationStatusResponse(
				"openai",
				false,
				List.of("org.springframework.ai.reader.pdf.PagePdfDocumentReader")
		));

		mockMvc.perform(get("/api/v1/ai/foundation"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.chatProvider").value("openai"))
				.andExpect(jsonPath("$.data.chatModelReady").value(false))
				.andExpect(jsonPath("$.data.documentReaders[0]").value("org.springframework.ai.reader.pdf.PagePdfDocumentReader"));
	}
}
