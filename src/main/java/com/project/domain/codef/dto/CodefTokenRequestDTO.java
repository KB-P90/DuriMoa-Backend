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
@ApiModel(description = "CODEF OAuth 토큰 요청 DTO")
public class CodefTokenRequestDTO {

    @ApiModelProperty(value = "CODEF Client ID (미입력시 서버 기본값 사용)", example = "your-client-id")
    private String clientId;

    @ApiModelProperty(value = "CODEF Client Secret (미입력시 서버 기본값 사용)", example = "your-client-secret")
    private String clientSecret;
}
