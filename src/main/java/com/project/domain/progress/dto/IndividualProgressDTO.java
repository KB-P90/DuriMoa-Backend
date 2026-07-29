package com.project.domain.progress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndividualProgressDTO {

    private String name;
    private Long savingAmount;
    private Long targetAmount;
    private Integer progressRate;
}
