package com.project.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "AI 결혼 예산 추천 요청")
public class WeddingBudgetRecommendationRequestDTO {

    @NotNull(message = "결혼 예정일은 필수입니다.")
    @Future(message = "결혼 예정일은 오늘 이후여야 합니다.")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "결혼 예정일", example = "2027-10-09", required = true)
    private LocalDate weddingDate;

    @NotBlank(message = "결혼 예정 지역은 필수입니다.")
    @Size(max = 50, message = "결혼 예정 지역은 50자 이하여야 합니다.")
    @ApiModelProperty(value = "결혼 예정 지역", example = "서울(강남)", required = true)
    private String region;
}
