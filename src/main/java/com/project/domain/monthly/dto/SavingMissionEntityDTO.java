package com.project.domain.monthly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingMissionEntityDTO {

    private Long id;
    private Long userId;
    private String categoryCode;
    private String title;
    private Long baselineAmount;
    private Long expectedSavingAmount;
    private String status;
    private LocalDateTime createdAt;
}
