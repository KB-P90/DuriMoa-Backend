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
@ApiModel(description = "온보딩 카드 승인 내역 DB 저장 요청 DTO")
public class OnboardingCardRecordCreateRequestDTO {

    @ApiModelProperty(value = "카드 거래내역 ID (PK, 자동 생성)")
    private Long id;

    @ApiModelProperty(value = "카드 PK (card_id)", required = true)
    private Long cardId;

    @ApiModelProperty(value = "거래 유형 (INCOME/EXPENSE)", required = true)
    private String type;

    @ApiModelProperty(value = "카테고리")
    private String category;

    @ApiModelProperty(value = "가맹점명/거래명", required = true)
    private String title;

    @ApiModelProperty(value = "승인/거래 금액", required = true)
    private Long amount;

    @ApiModelProperty(value = "거래 일자 (YYYY-MM-DD)", required = true)
    private String recordDate;

    @ApiModelProperty(value = "메모")
    private String memo;
}
