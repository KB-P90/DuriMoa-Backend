package com.project.domain.codef.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
@ApiModel(description = "CODEF OAuth 토큰 응답 DTO")
public class CodefTokenResponseDTO {

    @JsonProperty("access_token")
    @ApiModelProperty(value = "발급된 Access Token")
    private String accessToken;

    @JsonProperty("token_type")
    @ApiModelProperty(value = "토큰 타입", example = "Bearer")
    private String tokenType;

    @JsonProperty("expires_in")
    @ApiModelProperty(value = "유효 기간(초)", example = "604800")
    private Long expiresIn;

    @JsonProperty("scope")
    @ApiModelProperty(value = "권한 범위", example = "read")
    private String scope;
}
