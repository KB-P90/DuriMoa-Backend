package com.project.domain.codef.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "CODEF Connected ID 계정 추가 요청 DTO")
public class CodefConnectedIdAddRequestDTO {

    @NotBlank(message = "connectedId는 필수 항목입니다.")
    @ApiModelProperty(value = "기존 CODEF Connected ID", required = true, example = "demo_connected_id")
    private String connectedId;

    @NotEmpty(message = "추가할 계정 정보 목록은 필수입니다.")
    @Valid
    @ApiModelProperty(value = "추가할 금융기관 계정 정보 목록", required = true)
    private List<CodefAccountItemDTO> accountList;
}
