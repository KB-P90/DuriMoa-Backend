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
@ApiModel(description = "CODEF 은행 거래내역 항목 DTO")
public class CodefBankTransactionItemDTO {

    @JsonProperty("resAccountTrDate")
    @JsonAlias({"resTrDate", "resAccountTrDate"})
    @ApiModelProperty(value = "거래 일자 (YYYYMMDD)", example = "20260715")
    private String transactionDate;

    @JsonProperty("resAccountTrTime")
    @ApiModelProperty(value = "거래 시각 (HHMMSS)", example = "143000")
    private String transactionTime;

    @JsonProperty("resAccountOut")
    @ApiModelProperty(value = "출금액", example = "50000")
    private Long outAmount;

    @JsonProperty("resAccountIn")
    @ApiModelProperty(value = "입금액", example = "0")
    private Long inAmount;

    @JsonProperty("resAccountTrAmount")
    @ApiModelProperty(value = "거래 금액", example = "50000")
    private Long transactionAmount;

    @JsonProperty("resAccountDesc1")
    @ApiModelProperty(value = "거래 적요 1", example = "스타벅스")
    private String description1;

    @JsonProperty("resAccountDesc2")
    @ApiModelProperty(value = "거래 적요 2", example = "체크카드지출")
    private String description2;

    @JsonProperty("resAccountDesc3")
    @ApiModelProperty(value = "거래 적요 3", example = "메모")
    private String description3;
}
