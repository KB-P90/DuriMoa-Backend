package com.project.domain.codef.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "CODEF Connected ID 연동 계정 목록 조회 요청 DTO")
public class CodefConnectedIdAccountListRequestDTO {

    @NotBlank(message = "connectedId는 필수 항목입니다.")
    @ApiModelProperty(value = "조회할 CODEF Connected ID", required = true, example = "demo_connected_id")
    private String connectedId;
}
