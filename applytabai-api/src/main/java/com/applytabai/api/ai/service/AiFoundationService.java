package com.applytabai.api.ai.service;

import java.util.List;

import com.applytabai.api.ai.dto.AiFoundationStatusResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class AiFoundationService {

	private final ObjectProvider<ChatModel> chatModelProvider;

	public AiFoundationService(ObjectProvider<ChatModel> chatModelProvider) {
		this.chatModelProvider = chatModelProvider;
	}

	public AiFoundationStatusResponse getStatus() {
		return new AiFoundationStatusResponse(
				"openai",
				chatModelProvider.getIfAvailable() != null,
				List.of(PagePdfDocumentReader.class.getName())
		);
	}
}
