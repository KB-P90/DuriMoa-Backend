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
@ApiModel(description = "온보딩 계좌 등록 DB 저장 요청 DTO")
public class OnboardingAccountCreateRequestDTO {

    @ApiModelProperty(value = "계좌 ID (PK, 자동 생성)")
    private Long id;

    @ApiModelProperty(value = "사용자 ID", required = true)
    private Long userId;

    @ApiModelProperty(value = "금융회사명")
    private String company;

    @ApiModelProperty(value = "계좌 번호")
    private String number;

    @ApiModelProperty(value = "계좌 별칭/명칭")
    private String cardName;

    @ApiModelProperty(value = "계좌 ID (문자열)")
    private String accountId;

    @ApiModelProperty(value = "계좌 비밀번호")
    private String accountPassword;
}
