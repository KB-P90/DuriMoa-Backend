package com.project.domain.goal.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalStatisticsVO {

    private Long id;
    private String region;
    private String category;
    private Long high10;
    private Long high25;
    private Long mid50;
    private Long low25;
    private Long low10;
}
