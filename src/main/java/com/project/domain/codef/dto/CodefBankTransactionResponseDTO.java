package com.project.domain.codef.dto;

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
@ApiModel(description = "CODEF 은행 거래내역 응답 DTO")
public class CodefBankTransactionResponseDTO {

    @ApiModelProperty(value = "은행 거래내역 목록")
    private List<CodefBankTransactionItemDTO> items;

    public static CodefBankTransactionResponseDTO fromData(Object dataObj, ObjectMapper objectMapper) {
        if (dataObj == null) {
            return CodefBankTransactionResponseDTO.builder().items(Collections.emptyList()).build();
        }
        List<CodefBankTransactionItemDTO> itemList = new ArrayList<>();
        try {
            if (dataObj instanceof List) {
                itemList = objectMapper.convertValue(dataObj, new TypeReference<List<CodefBankTransactionItemDTO>>() {});
            } else if (dataObj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) dataObj;
                for (Object val : map.values()) {
                    if (val instanceof List) {
                        itemList = objectMapper.convertValue(val, new TypeReference<List<CodefBankTransactionItemDTO>>() {});
                        break;
                    }
                }
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(CodefBankTransactionResponseDTO.class)
                    .error("[CODEF-PARSE-ERROR] 은행 거래내역 데이터 DTO 변환 중 오류 발생: {}", e.getMessage(), e);
        }
        return CodefBankTransactionResponseDTO.builder()
                .items(itemList != null ? itemList : Collections.emptyList())
                .build();
    }
}
