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
@ApiModel(description = "CODEF 카드 승인내역 응답 DTO")
public class CodefCardApprovalResponseDTO {

    @ApiModelProperty(value = "카드 승인내역 목록")
    private List<CodefCardApprovalItemDTO> items;

    public static CodefCardApprovalResponseDTO fromData(Object dataObj, ObjectMapper objectMapper) {
        if (dataObj == null) {
            return CodefCardApprovalResponseDTO.builder().items(Collections.emptyList()).build();
        }
        List<CodefCardApprovalItemDTO> itemList = new ArrayList<>();
        try {
            if (dataObj instanceof List) {
                itemList = objectMapper.convertValue(dataObj, new TypeReference<List<CodefCardApprovalItemDTO>>() {});
            } else if (dataObj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) dataObj;
                for (Object val : map.values()) {
                    if (val instanceof List) {
                        itemList = objectMapper.convertValue(val, new TypeReference<List<CodefCardApprovalItemDTO>>() {});
                        break;
                    }
                }
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(CodefCardApprovalResponseDTO.class)
                    .error("[CODEF-PARSE-ERROR] 카드 승인내역 데이터 DTO 변환 중 오류 발생: {}", e.getMessage(), e);
        }
        return CodefCardApprovalResponseDTO.builder()
                .items(itemList != null ? itemList : Collections.emptyList())
                .build();
    }
}
