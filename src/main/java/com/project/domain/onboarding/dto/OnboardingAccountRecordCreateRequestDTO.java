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
@ApiModel(description = "온보딩 계좌 거래 내역 DB 저장 요청 DTO")
public class OnboardingAccountRecordCreateRequestDTO {

    @ApiModelProperty(value = "거래내역 ID (PK, 자동 생성)")
    private Long id;

    @ApiModelProperty(value = "계좌 PK (account_id)", required = true)
    private Long accountId;

    @ApiModelProperty(value = "거래 유형 (INCOME/EXPENSE)", required = true)
    private String type;

    @ApiModelProperty(value = "카테고리")
    private String category;

    @ApiModelProperty(value = "거래 내역 명칭/적요", required = true)
    private String title;

    @ApiModelProperty(value = "거래 금액", required = true)
    private Long amount;

    @ApiModelProperty(value = "거래 일자 (YYYY-MM-DD)", required = true)
    private String recordDate;

    @ApiModelProperty(value = "메모")
    private String memo;
}
