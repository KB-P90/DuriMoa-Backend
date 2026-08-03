package com.project.domain.progress.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveGoalDTO {

    private Long goalId;
    private Long targetAmount;
    private Long groomRatio;
    private Long brideRatio;
    private LocalDateTime createdAt;
}
