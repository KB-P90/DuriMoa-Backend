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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(description = "CODEF 은행 거래내역 항목 DTO")
public class CodefBankTransactionItemDTO {

    @JsonProperty("resAccountTrDate")
    @JsonAlias({"resTrDate", "resAccountTrDate", "trDate", "resUsedDate"})
    @ApiModelProperty(value = "거래 일자 (YYYYMMDD)", example = "20260715")
    private String transactionDate;

    @JsonProperty("resAccountTrTime")
    @JsonAlias({"resTrTime", "resAccountTrTime", "trTime"})
    @ApiModelProperty(value = "거래 시각 (HHMMSS)", example = "143000")
    private String transactionTime;

    @JsonProperty("resAccountOut")
    @JsonAlias({"resOut", "resAccountOut", "outAmount"})
    @ApiModelProperty(value = "출금액", example = "50000")
    private Long outAmount;

    @JsonProperty("resAccountIn")
    @JsonAlias({"resIn", "resAccountIn", "inAmount"})
    @ApiModelProperty(value = "입금액", example = "0")
    private Long inAmount;

    @JsonProperty("resAccountTrAmount")
    @JsonAlias({"resTrAmount", "resAccountTrAmount", "trAmount"})
    @ApiModelProperty(value = "거래 금액", example = "50000")
    private Long transactionAmount;

    @JsonProperty("resAccountDesc1")
    @JsonAlias({"resDesc1", "resAccountDesc1", "desc1"})
    @ApiModelProperty(value = "거래 적요 1", example = "스타벅스")
    private String description1;

    @JsonProperty("resAccountDesc2")
    @JsonAlias({"resDesc2", "resAccountDesc2", "desc2"})
    @ApiModelProperty(value = "거래 적요 2", example = "체크카드지출")
    private String description2;

    @JsonProperty("resAccountDesc3")
    @JsonAlias({"resDesc3", "resAccountDesc3", "desc3"})
    @ApiModelProperty(value = "거래 적요 3 (적요)", example = "메모")
    private String description3;

    @JsonProperty("resAccountDesc4")
    @JsonAlias({"resDesc4", "resAccountDesc4", "desc4"})
    @ApiModelProperty(value = "거래 적요 4 (거래점)", example = "여의도지점")
    private String description4;

    @JsonProperty("resAfterTranBalance")
    @JsonAlias({"resAfterTranBalance", "afterTranBalance"})
    @ApiModelProperty(value = "거래후 잔액", example = "11517")
    private Long afterTranBalance;
}
