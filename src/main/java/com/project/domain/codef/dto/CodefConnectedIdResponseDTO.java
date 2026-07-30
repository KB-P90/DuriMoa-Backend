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
@ApiModel(description = "CODEF Connected ID 생성 응답 DTO")
public class CodefConnectedIdResponseDTO {

    @ApiModelProperty(value = "발급된 Connected ID")
    private String connectedId;
}
