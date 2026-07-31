package com.project.domain.onboarding.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "온보딩 계좌/카드 목록 조회 응답 DTO")
public class OnboardingQueryResponseDTO {

    @ApiModelProperty(value = "CODEF Connected ID", example = "demo_connected_id")
    private String connectedId;

    @ApiModelProperty(value = "업무 구분 (BK: 은행, CD: 카드)", example = "BK")
    private String businessType;

    @ApiModelProperty(value = "기관 코드", example = "0004")
    private String organization;

    @ApiModelProperty(value = "특정 기관의 계좌/카드 조회 결과 데이터 항목 목록")
    private Object itemList;
}
