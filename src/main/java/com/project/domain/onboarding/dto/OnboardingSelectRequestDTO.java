package com.project.domain.onboarding.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "온보딩 계좌/카드 선택 및 거래 내역 저장 요청 DTO")
public class OnboardingSelectRequestDTO {

    @NotBlank(message = "금융기관 이름(company)은 필수입니다.")
    @ApiModelProperty(value = "금융기관 이름 또는 코드 (예: KB국민은행, KB국민카드)", required = true, example = "KB국민은행")
    private String company;

    @NotEmpty(message = "선택한 계좌/카드 번호 목록은 필수입니다.")
    @ApiModelProperty(value = "사용자가 선택한 계좌번호 또는 카드번호 목록", required = true, example = "[\"05520204230452\"]")
    private List<String> selectedNumbers;

    @ApiModelProperty(value = "조회 시작일 (YYYYMMDD, 미입력 시 최근 3개월 전)", example = "20260501")
    private String startDate;

    @ApiModelProperty(value = "조회 종료일 (YYYYMMDD, 미입력 시 오늘 날짜)", example = "20260731")
    private String endDate;
}
