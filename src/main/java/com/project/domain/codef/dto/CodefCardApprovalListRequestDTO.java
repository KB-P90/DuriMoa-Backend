package com.project.domain.codef.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description = "CODEF 카드 승인내역 조회 요청 DTO")
public class CodefCardApprovalListRequestDTO {

    @ApiModelProperty(value = "기관 코드 (예: 0301 KB국민카드, 0302 현대카드, 0309 우리카드)", example = "0309")
    private String organization;

    @NotBlank(message = "connectedId는 필수 항목입니다.")
    @ApiModelProperty(value = "CODEF Connected ID", required = true, example = "demo_connected_id")
    private String connectedId;

    @ApiModelProperty(value = "생년월일 (YYMMDD 또는 YYYYMMDD)", example = "19950101")
    private String birthDate;

    @ApiModelProperty(value = "조회 시작일자 (YYYYMMDD)", example = "20260701")
    private String startDate;

    @ApiModelProperty(value = "조회 종료일자 (YYYYMMDD)", example = "20260731")
    private String endDate;

    @Builder.Default
    @ApiModelProperty(value = "일자정렬순서 (0: 최신순, 1: 과거순)", example = "0")
    private String orderBy = "0";

    @Builder.Default
    @ApiModelProperty(value = "조회구분 (0: 카드별 조회, 1: 전체조회)", example = "0")
    private String inquiryType = "0";

    @ApiModelProperty(value = "카드명 (inquiryType='0'인 경우)", example = "우리카드")
    private String cardName;

    @ApiModelProperty(value = "중복카드 일련번호 (inquiryType='0'인 경우)", example = "0")
    private String duplicateCardIdx;

    @ApiModelProperty(value = "카드번호 (현대카드 아이디로그인 필수 / KB 카드소지확인용)", example = "\"1234567890123456\"", dataType = "String")
    private String cardNo;

    @ApiModelProperty(value = "카드 비밀번호 (RSA 암호화된 비밀번호 - 현대카드 4자리 / KB 앞2자리)", example = "rsa_encrypted_password")
    private String cardPassword;

    @Builder.Default
    @ApiModelProperty(value = "가맹점정보 포함 여부 (0: 미포함, 1: 가맹점 포함, 2: 부가세 포함, 3: 전체 포함)", example = "0")
    private String memberStoreInfoType = "0";
}
