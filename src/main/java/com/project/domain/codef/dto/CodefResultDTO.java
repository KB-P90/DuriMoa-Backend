package com.project.domain.codef.dto;

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
@ApiModel(description = "CODEF API 공통 결과 DTO")
public class CodefResultDTO {

    @ApiModelProperty(value = "응답 코드 (성공시 CF-00000)", example = "CF-00000")
    private String code;

    @ApiModelProperty(value = "응답 메시지", example = "성공")
    private String message;

    @ApiModelProperty(value = "추가 응답 메시지")
    private String extraMessage;

    @ApiModelProperty(value = "트랜잭션 ID")
    private String transactionId;
}
