package com.project.domain.goal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalCreateResponseDTO {

    private Long goalId;
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate weddingDate;

    private String region;
    private Long totalBudget;
    private Long groomRatio;
    private Long brideRatio;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private List<GoalCreateItemResponseDTO> items;
}
