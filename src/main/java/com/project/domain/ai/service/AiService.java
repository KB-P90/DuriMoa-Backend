package com.project.domain.ai.service;

import com.project.domain.ai.dto.AiChatResponseDTO;
import com.project.domain.ai.dto.AiBudgetTypeModelResponseDTO;
import com.project.domain.ai.dto.AiConnectionTestResponseDTO;
import com.project.domain.ai.model.AiWeddingBudgetSelectionContext;

public interface AiService {

    AiConnectionTestResponseDTO testConnection();

    AiChatResponseDTO chat(String message);

    AiBudgetTypeModelResponseDTO selectWeddingBudgetType(
            AiWeddingBudgetSelectionContext context
    );
}
