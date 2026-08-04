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
@ApiModel(description = "CODEF 은행 거래내역 조회 요청 DTO")
public class CodefBankTransactionListRequestDTO {

    @NotBlank(message = "connectedId는 필수 항목입니다.")
    @ApiModelProperty(value = "CODEF Connected ID", required = true, example = "demo_connected_id")
    private String connectedId;

    @ApiModelProperty(value = "기관 코드 (예: 0004 KB국민은행)", example = "0004")
    private String organization;

    @NotBlank(message = "account는 필수 항목입니다.")
    @ApiModelProperty(value = "계좌번호", required = true, example = "\"12345678901234\"")
    private String account;

    @ApiModelProperty(value = "조회 시작일자 (YYYYMMDD)", example = "20260701")
    private String startDate;

    @ApiModelProperty(value = "조회 종료일자 (YYYYMMDD)", example = "20260731")
    private String endDate;

    @Builder.Default
    @ApiModelProperty(value = "정렬 순서 (0: 최신순, 1: 과거순)", example = "0")
    private String orderBy = "0";

    @Builder.Default
    @ApiModelProperty(value = "조회 구분 (1: 전체)", example = "1")
    private String inquireType = "1";
}
