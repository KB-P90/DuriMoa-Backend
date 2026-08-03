package com.project.domain.codef.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@ApiModel(description = "CODEF 카드 승인내역 항목 DTO")
public class CodefCardApprovalItemDTO {

    @JsonProperty("resUsedDate")
    @JsonAlias({"resApprovedDate", "resUsedDate"})
    @ApiModelProperty(value = "사용/승인 일자 (YYYYMMDD)", example = "20260715")
    private String usedDate;

    @JsonProperty("resUsedTime")
    @JsonAlias({"resApprovedTime", "resUsedTime"})
    @ApiModelProperty(value = "사용/승인 시각 (HHMMSS)", example = "143000")
    private String usedTime;

    @JsonProperty("resUsedAmount")
    @JsonAlias({"resApprovedAmount", "resUsedAmount"})
    @ApiModelProperty(value = "사용/승인 금액", example = "15000")
    private Long usedAmount;

    @JsonProperty("resMemberStoreName")
    @JsonAlias({"resStoreName", "resMemberStoreName"})
    @ApiModelProperty(value = "가맹점명", example = "배달의민족")
    private String storeName;
}
