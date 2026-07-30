package com.project.domain.progress.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressResponseDTO {

    private Long totalSavingAmount;
    private Long targetAmount;
    private Integer progressRate;
    private Long groomSavingAmount;
    private Long brideSavingAmount;
    private List<MonthlySavingDTO> monthlySavings;
}
