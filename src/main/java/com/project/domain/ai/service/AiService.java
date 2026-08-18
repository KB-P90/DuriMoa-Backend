package com.project.domain.ai.service;

import com.project.domain.ai.dto.AiChatResponseDTO;
import com.project.domain.ai.dto.AiConnectionTestResponseDTO;

public interface AiService {

    AiConnectionTestResponseDTO testConnection();

    AiChatResponseDTO chat(String message);
}
