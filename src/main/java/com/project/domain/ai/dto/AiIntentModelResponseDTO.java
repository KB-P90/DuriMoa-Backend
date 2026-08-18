package com.project.domain.ai.dto;

import com.project.domain.ai.domain.AiIntent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiIntentModelResponseDTO {

    private AiIntent intent;
    private String response;
}
