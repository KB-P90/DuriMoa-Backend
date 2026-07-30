package com.project.domain.codef.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "CODEF API 상품 범용 요청 DTO")
public class CodefProductRequestDTO {

    @NotBlank(message = "productUrl은 필수 항목입니다.")
    @ApiModelProperty(value = "요청 상품 URL 엔드포인트", required = true, example = "/v1/kr/bank/p/account/account-list")
    private String productUrl;

    @ApiModelProperty(value = "서비스 환경 구분 (SANDBOX, DEMO, PRODUCT)", example = "SANDBOX")
    private String serviceType;

    @ApiModelProperty(value = "요청 파라미터 맵 (JSON 구조)")
    private Map<String, Object> parameterMap;
}
