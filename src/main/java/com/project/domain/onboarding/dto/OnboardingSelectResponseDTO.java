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
@ApiModel(description = "온보딩 계좌/카드 선택 및 거래 내역 저장 응답 DTO")
public class OnboardingSelectResponseDTO {

    @ApiModelProperty(value = "업무 구분 (BK: 은행, CD: 카드)", example = "BK")
    private String businessType;

    @ApiModelProperty(value = "기관 코드", example = "0004")
    private String organization;

    @ApiModelProperty(value = "DB에 성공적으로 저장된 총 거래 내역 수", example = "42")
    private int totalSavedRecords;

    @ApiModelProperty(value = "거래 내역 동기화가 완료된 계좌/카드 번호 목록")
    private List<String> processedNumbers;
}
