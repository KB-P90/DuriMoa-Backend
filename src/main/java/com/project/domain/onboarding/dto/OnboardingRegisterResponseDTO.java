package com.project.domain.onboarding.dto;

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
@ApiModel(description = "온보딩 계좌/카드 등록 및 추가 응답 DTO")
public class OnboardingRegisterResponseDTO {

    @ApiModelProperty(value = "생성/연동된 CODEF Connected ID", example = "demo_connected_id")
    private String connectedId;

    @ApiModelProperty(value = "수행된 작업 유형 (CREATED: 신규 등록, ADDED: 계정 추가)", example = "CREATED")
    private String actionType;

    @ApiModelProperty(value = "업무 구분 (BK: 은행, CD: 카드)", example = "BK")
    private String businessType;

    @ApiModelProperty(value = "기관 코드", example = "0004")
    private String organization;

    @ApiModelProperty(value = "금융기관명", example = "국민은행")
    private String company;
}
