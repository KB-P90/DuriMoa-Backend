package com.project.domain.codef.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(description = "CODEF Connected ID 생성 응답 DTO")
public class CodefConnectedIdResponseDTO {

    @ApiModelProperty(value = "발급된 Connected ID")
    private String connectedId;

    @ApiModelProperty(value = "성공 계정 목록")
    private List<CodefConnectedSuccessItemDTO> successList;

    public String getFirstAccountNumber() {
        if (successList == null || successList.isEmpty()) {
            return null;
        }
        for (CodefConnectedSuccessItemDTO item : successList) {
            if (item.getResDepositAccountList() != null && !item.getResDepositAccountList().isEmpty()) {
                Object acc = item.getResDepositAccountList().get(0).get("resAccount");
                if (acc == null) acc = item.getResDepositAccountList().get(0).get("resAccountNum");
                if (acc != null) return acc.toString();
            }
            if (item.getResAccountList() != null && !item.getResAccountList().isEmpty()) {
                Object acc = item.getResAccountList().get(0).get("resAccount");
                if (acc == null) acc = item.getResAccountList().get(0).get("resAccountNum");
                if (acc != null) return acc.toString();
            }
        }
        return null;
    }

    public String getFirstCardNumber() {
        if (successList == null || successList.isEmpty()) {
            return null;
        }
        for (CodefConnectedSuccessItemDTO item : successList) {
            if (item.getResCardList() != null && !item.getResCardList().isEmpty()) {
                Object card = item.getResCardList().get(0).get("resCardNo");
                if (card == null) card = item.getResCardList().get(0).get("resCardNum");
                if (card != null) return card.toString();
            }
        }
        return null;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CodefConnectedSuccessItemDTO {
        private String organization;
        private List<Map<String, Object>> resDepositAccountList;
        private List<Map<String, Object>> resAccountList;
        private List<Map<String, Object>> resCardList;
    }
}
