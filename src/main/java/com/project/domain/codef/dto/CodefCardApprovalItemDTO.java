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
@ApiModel(description = "CODEF 카드 승인내역 항목 DTO")
public class CodefCardApprovalItemDTO {

    @JsonProperty("resUsedDate")
    @JsonAlias({"resApprovedDate", "resUsedDate", "usedDate", "approvedDate"})
    @ApiModelProperty(value = "사용/승인 일자 (YYYYMMDD)", example = "20260715")
    private String usedDate;

    @JsonProperty("resUsedTime")
    @JsonAlias({"resApprovedTime", "resUsedTime", "usedTime", "approvedTime"})
    @ApiModelProperty(value = "사용/승인 시각 (HHMMSS)", example = "143000")
    private String usedTime;

    @JsonProperty("resMemberStoreName")
    @JsonAlias({"resStoreName", "resMemberStoreName", "storeName", "resStore"})
    @ApiModelProperty(value = "가맹점명", example = "배달의민족")
    private String storeName;

    @JsonProperty("resUsedAmount")
    @JsonAlias({"resApprovedAmount", "resUsedAmount", "usedAmount", "approvedAmount"})
    @ApiModelProperty(value = "사용/승인 금액", example = "15000")
    private Long usedAmount;

    @JsonProperty("resPaymentType")
    @ApiModelProperty(value = "결제 방법", example = "1:일시불, 2:할부, 3:그외")
    private String paymentType;

    @JsonProperty("resInstallmentMonth")
    @ApiModelProperty(value = "할부 개월")
    private Integer installmentMonth;

    @JsonProperty("resApprovalNo")
    @ApiModelProperty(value = "승인 번호", example="30012351")
    private String approvalNo;

    @JsonProperty("resPaymentDueDate")
    @ApiModelProperty(value = "결제 예정일 (YYYYMMDD)")
    private String paymentDueDate;

    @JsonProperty("resHomeForeignType")
    @ApiModelProperty(value = "국내/해외 구분", example = "1:국내, 2:해외")
    private String domesticForeignType;

    @JsonProperty("resMemberStoreCorpNo")
    @ApiModelProperty(value = "가맹점 사업자 번호")
    private String storeCorpNo;

    @JsonProperty("resMemberStoreType")
    @ApiModelProperty(value = "가맹점 업종")
    private String storeType;

    @JsonProperty("resMemberStoreNo")
    @ApiModelProperty(value = "가맹점 번호")
    private String storeNo;

    @JsonProperty("resCancelYN")
    @ApiModelProperty(value = "취소 여부", example="0:정상, 1:취소, 2: 부분취소, 3:거절")
    private String cancelYn;

    @JsonProperty("resCancelAmount")
    @ApiModelProperty(value = "취소 금액")
    private Long cancelAmount;

    @JsonProperty("resVAT")
    @ApiModelProperty(value = "부가세")
    private Long vat;

    @JsonProperty("resCashBack")
    @ApiModelProperty(value = "캐시백/봉사료")
    private Long cashBack;

    @JsonProperty("resKRWAmt")
    @ApiModelProperty(value = "원화 금액")
    private Long krwAmount;

    @JsonProperty("resAccountCurrency")
    @ApiModelProperty(value = "통화 코드", example="KRW:한국원화, JPY:일본엔, USD:미국달러, EUR:유로, ...")
    private String currency;
}
