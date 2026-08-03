package com.project.domain.onboarding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description = "온보딩 계좌/카드 목록 조회 응답 DTO")
public class OnboardingQueryResponseDTO {

    @ApiModelProperty(value = "특정 기관의 계좌/카드 목록 (1차원 배열)")
    private Object itemList;
}
