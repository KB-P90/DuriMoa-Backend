package com.project.domain.goal.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalVO {

    private Long id;
    private Long coupleId;
    private String name;
    private LocalDate weddingDate;
    private String region;
    private Long totalBudget;
    private Long groomRatio;
    private Long brideRatio;
    private String status;
    private Boolean isMain;
    private LocalDateTime createdAt;
}
