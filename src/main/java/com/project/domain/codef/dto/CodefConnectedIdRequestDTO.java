package com.project.domain.codef.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "CODEF Connected ID 생성 요청 DTO")
public class CodefConnectedIdRequestDTO {

    @NotEmpty(message = "계정 정보 목록은 필수입니다.")
    @ApiModelProperty(value = "연동할 금융기관 계정 정보 목록 (password 항목에 평문 비밀번호를 입력하면 백엔드에서 자동으로 RSA 암호화 처리됩니다)", required = true)
    private List<Map<String, Object>> accountList;
}
