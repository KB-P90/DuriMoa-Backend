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

    @ApiModelProperty(value = "서비스 환경 구분 (DEMO, SANDBOX, PRODUCT)", example = "DEMO")
    @Builder.Default
    private String serviceType = "DEMO";

    @ApiModelProperty(value = "요청 파라미터 맵 (예시: {\"connectedId\": \"demo_connected_id\", \"organization\": \"0004\"}) \n" +
            "- connectedId (필수): CODEF Connected ID \n" +
            "- organization (선택): 금융기관 코드 (예: 0004 KB국민은행) \n" +
            "- startDate / endDate (거래내역 조회 시): YYYYMMDD 날짜 \n" +
            "- account (계좌/카드 조회 시): 계좌번호 또는 카드번호")
    private Map<String, Object> parameterMap;
}
