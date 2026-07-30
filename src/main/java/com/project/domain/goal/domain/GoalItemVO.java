package com.project.domain.goal.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalItemVO {

    private Long id;
    private Long goalId;
    private String category;
    private Long targetAmount;
    private Boolean included;
    private Boolean completed;
    private String type;
}
