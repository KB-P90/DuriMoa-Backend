package com.project.domain.calendar.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@ApiModel(description = "거래 내역 등록 및 수정 응답 DTO")
public class RecordResponseDTO {
    @ApiModelProperty(value = "등록 또는 수정된 거래 내역 ID", example = "15")
    private Long recordId;

    public static RecordResponseDTO of(Long recordId) {
        return new RecordResponseDTO(recordId);
    }
}
