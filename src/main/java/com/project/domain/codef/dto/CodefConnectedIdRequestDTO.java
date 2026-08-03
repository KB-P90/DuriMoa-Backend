package com.project.domain.codef.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "CODEF Connected ID 생성 요청 DTO")
public class CodefConnectedIdRequestDTO {

    @NotEmpty(message = "계정 정보 목록은 필수입니다.")
    @Valid
    @ApiModelProperty(value = "연동할 금융기관 계정 정보 목록", required = true)
    private List<CodefAccountItemDTO> accountList;
}
