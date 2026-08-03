package com.project.domain.progress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalItemSourceDTO {

    private Long goalItemId;
    private String category;
    private Long targetAmount;
    private Boolean completed;
}
