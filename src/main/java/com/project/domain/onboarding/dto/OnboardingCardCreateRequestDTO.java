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
@ApiModel(description = "온보딩 카드 등록 DB 저장 요청 DTO")
public class OnboardingCardCreateRequestDTO {

    @ApiModelProperty(value = "카드 ID (PK, 자동 생성)")
    private Long id;

    @ApiModelProperty(value = "사용자 ID", required = true)
    private Long userId;

    @ApiModelProperty(value = "카드 상품 ID")
    private Long cardProductId;

    @ApiModelProperty(value = "카드사명")
    private String company;

    @ApiModelProperty(value = "카드 번호")
    private String number;

    @ApiModelProperty(value = "카드 별칭/명칭")
    private String cardName;

    @ApiModelProperty(value = "카드 ID (문자열)")
    private String cardId;

    @ApiModelProperty(value = "카드 비밀번호")
    private String cardPassword;
}
