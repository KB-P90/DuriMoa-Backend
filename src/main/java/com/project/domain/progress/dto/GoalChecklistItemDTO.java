package com.project.domain.progress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalChecklistItemDTO {

    private String category;
    private Long targetAmount;
    private Long currentAmount;
    private Integer progressRate;
    private Boolean completed;
}
