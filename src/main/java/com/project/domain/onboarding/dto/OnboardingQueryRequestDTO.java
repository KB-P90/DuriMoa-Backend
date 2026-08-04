package com.project.domain.onboarding.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "온보딩 계좌/카드 목록 조회 요청 DTO")
public class OnboardingQueryRequestDTO {

    @NotBlank(message = "금융기관 이름(company)은 필수입니다.")
    @ApiModelProperty(value = "금융기관 이름 또는 코드 (예: KB국민은행, KB국민카드)", required = true, example = "KB국민은행")
    private String company;
}
