package com.project.domain.codef.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description = "CODEF 카드 승인내역 조회 요청 DTO")
public class CodefCardApprovalListRequestDTO {

    @NotBlank(message = "connectedId는 필수 항목입니다.")
    @ApiModelProperty(value = "CODEF Connected ID", required = true, example = "demo_connected_id")
    private String connectedId;

    @ApiModelProperty(value = "기관 코드 (예: 0301 KB국민카드)", example = "0301")
    private String organization;

    @ApiModelProperty(value = "카드번호", example = "1234-5678-****-****")
    private String cardNumber;

    @ApiModelProperty(value = "조회 시작일자 (YYYYMMDD)", example = "20260701")
    private String startDate;

    @ApiModelProperty(value = "조회 종료일자 (YYYYMMDD)", example = "20260731")
    private String endDate;

    @Builder.Default
    @ApiModelProperty(value = "조회 구분 (1: 승인내역)", example = "1")
    private String inquireType = "1";
}
