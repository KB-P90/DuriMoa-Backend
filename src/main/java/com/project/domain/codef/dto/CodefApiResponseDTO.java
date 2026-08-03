package com.project.domain.codef.dto;

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
@ApiModel(description = "CODEF API 공통 응답 래퍼 DTO")
public class CodefApiResponseDTO<T> {

    @ApiModelProperty(value = "CODEF 처리 결과 정보")
    private CodefResultDTO result;

    @ApiModelProperty(value = "CODEF 처리 데이터 객체")
    private T data;
}
