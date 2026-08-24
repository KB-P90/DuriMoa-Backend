package com.project.domain.strategy.card.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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
@ApiModel(description = "외부 카드 상품 크롤링 요청 DTO")
public class CardProductCrawlRequestDTO {

    // 한 번에 수집할 최대 카드 상품 수입니다.
    @JsonProperty("max_count")
    @JsonAlias("maxCount")
    @ApiModelProperty(value = "수집할 최대 카드 상품 수", example = "100")
    private Integer maxCount;

    // 수집할 카드 유형입니다. ALL이면 신용카드와 체크카드를 함께 수집합니다.
    @JsonProperty("card_type")
    @JsonAlias("cardType")
    @ApiModelProperty(value = "수집할 카드 유형(ALL, CRD, CHK)", example = "ALL")
    private String cardType;
}
