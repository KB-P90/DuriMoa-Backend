package com.project.domain.ai.dto;

import com.project.domain.ai.domain.AiAction;
import com.project.domain.ai.domain.AiIntent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponseDTO {

    private String model;
    private String response;
    private AiIntent intent;
    private AiAction action;
}
