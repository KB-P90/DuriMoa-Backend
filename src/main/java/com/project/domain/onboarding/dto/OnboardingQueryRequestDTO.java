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

    @NotBlank(message = "업무 구분(BK: 은행, CD: 카드)은 필수입니다.")
    @ApiModelProperty(value = "업무 구분 (BK: 은행, CD: 카드)", required = true, example = "BK")
    private String businessType;

    @NotBlank(message = "기관 코드는 필수입니다.")
    @ApiModelProperty(value = "기관 코드 (예: 0004 KB국민은행, 0301 KB국민카드)", required = true, example = "0004")
    private String organization;

    @ApiModelProperty(value = "CODEF Connected ID (미입력 시 사용자 DB 등록값 자동 사용)", example = "demo_connected_id")
    private String connectedId;
}
