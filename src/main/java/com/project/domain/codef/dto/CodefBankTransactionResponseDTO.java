package com.project.domain.codef.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(description = "CODEF 은행 거래내역 응답 DTO")
public class CodefBankTransactionResponseDTO {

    @JsonProperty("resAccountBalance")
    @ApiModelProperty(value = "현재잔액", example = "546")
    private String resAccountBalance;

    @JsonProperty("resWithdrawalAmt")
    @ApiModelProperty(value = "출금가능금액", example = "546")
    private String resWithdrawalAmt;

    @JsonProperty("resAccount")
    @ApiModelProperty(value = "계좌번호", example = "02004130000000")
    private String resAccount;

    @JsonProperty("resAccountNickName")
    @ApiModelProperty(value = "계좌별칭", example = "")
    private String resAccountNickName;

    @JsonProperty("resAccountStartDate")
    @ApiModelProperty(value = "신규일", example = "20081224")
    private String resAccountStartDate;

    @JsonProperty("resLastTranDate")
    @ApiModelProperty(value = "최종거래일", example = "20190624")
    private String resLastTranDate;

    @JsonProperty("resLoanLimitAmt")
    @ApiModelProperty(value = "대출한도", example = "")
    private String resLoanLimitAmt;

    @JsonProperty("resAccountName")
    @ApiModelProperty(value = "계좌명(종류)", example = "")
    private String resAccountName;

    @JsonProperty("resAccountHolder")
    @ApiModelProperty(value = "예금주", example = "")
    private String resAccountHolder;

    @JsonProperty("resManagementBranch")
    @ApiModelProperty(value = "관리지점", example = "여의도지점")
    private String resManagementBranch;

    @JsonProperty("resInterestRate")
    @ApiModelProperty(value = "대출이율", example = "")
    private String resInterestRate;

    @JsonProperty("resAccountDisplay")
    @ApiModelProperty(value = "계좌번호_표시용", example = "020-0413-0000-000")
    private String resAccountDisplay;

    @JsonProperty("resLoanEndDate")
    @ApiModelProperty(value = "대출만기일", example = "")
    private String resLoanEndDate;

    @JsonProperty("resAccountStatus")
    @ApiModelProperty(value = "계좌상태", example = "정상")
    private String resAccountStatus;

    @JsonProperty("commStartDate")
    @ApiModelProperty(value = "시작일자", example = "20190601")
    private String commStartDate;

    @JsonProperty("commEndDate")
    @ApiModelProperty(value = "종료일자", example = "20190619")
    private String commEndDate;

    @JsonProperty("resTrHistoryList")
    @ApiModelProperty(value = "은행 거래내역 목록")
    private List<CodefBankTransactionItemDTO> items;

    public static CodefBankTransactionResponseDTO fromData(Object dataObj, ObjectMapper objectMapper) {
        if (dataObj == null) {
            return CodefBankTransactionResponseDTO.builder().items(Collections.emptyList()).build();
        }
        List<CodefBankTransactionItemDTO> itemList = new ArrayList<>();
        CodefBankTransactionResponseDTO responseDTO = null;

        try {
            if (dataObj instanceof List) {
                itemList = objectMapper.convertValue(dataObj, new TypeReference<List<CodefBankTransactionItemDTO>>() {});
                responseDTO = CodefBankTransactionResponseDTO.builder().items(itemList).build();
            } else if (dataObj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) dataObj;
                Object historyListObj = map.get("resTrHistoryList");

                if (historyListObj instanceof List) {
                    itemList = objectMapper.convertValue(historyListObj, new TypeReference<List<CodefBankTransactionItemDTO>>() {});
                } else {
                    for (Object val : map.values()) {
                        if (val instanceof List) {
                            itemList = objectMapper.convertValue(val, new TypeReference<List<CodefBankTransactionItemDTO>>() {});
                            break;
                        }
                    }
                }

                responseDTO = objectMapper.convertValue(map, CodefBankTransactionResponseDTO.class);
                if (responseDTO == null) {
                    responseDTO = CodefBankTransactionResponseDTO.builder().build();
                }
                responseDTO.setItems(itemList != null ? itemList : Collections.emptyList());
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(CodefBankTransactionResponseDTO.class)
                    .error("[CODEF-PARSE-ERROR] 은행 거래내역 데이터 DTO 변환 중 오류 발생: {}", e.getMessage(), e);
        }

        if (responseDTO == null) {
            responseDTO = CodefBankTransactionResponseDTO.builder()
                    .items(itemList != null ? itemList : Collections.emptyList())
                    .build();
        }
        return responseDTO;
    }
}
