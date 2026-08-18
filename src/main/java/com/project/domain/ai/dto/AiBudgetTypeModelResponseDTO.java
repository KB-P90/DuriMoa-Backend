package com.project.domain.ai.dto;

import com.project.domain.ai.domain.AiBudgetType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiBudgetTypeModelResponseDTO {

    private AiBudgetType budgetType;
    private String reason;
}
