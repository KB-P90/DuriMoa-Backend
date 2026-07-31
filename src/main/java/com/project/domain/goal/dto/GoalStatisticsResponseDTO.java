package com.project.domain.goal.dto;

import com.project.domain.goal.domain.GoalStatisticsVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "지역·카테고리별 결혼 비용 통계 응답")
public class GoalStatisticsResponseDTO {

    @ApiModelProperty(value = "지역", example = "서울(강남)")
    private String region;

    @ApiModelProperty(value = "결혼 비용 카테고리", example = "예식장")
    private String category;

    @ApiModelProperty(value = "상위 10% 기준 금액", example = "13000000")
    private Long upper10;

    @ApiModelProperty(value = "상위 25% 기준 금액", example = "9200000")
    private Long upper25;

    @ApiModelProperty(value = "중간값", example = "7500000")
    private Long median;

    @ApiModelProperty(value = "하위 25% 기준 금액", example = "4400000")
    private Long lower25;

    @ApiModelProperty(value = "하위 10% 기준 금액", example = "2120000")
    private Long lower10;

    public static GoalStatisticsResponseDTO from(GoalStatisticsVO statistics) {
        return GoalStatisticsResponseDTO.builder()
                .region(statistics.getRegion())
                .category(statistics.getCategory())
                .upper10(statistics.getHigh10())
                .upper25(statistics.getHigh25())
                .median(statistics.getMid50())
                .lower25(statistics.getLow25())
                .lower10(statistics.getLow10())
                .build();
    }
}
