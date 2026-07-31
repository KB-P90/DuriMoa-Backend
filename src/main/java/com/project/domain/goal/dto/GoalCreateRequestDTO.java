package com.project.domain.goal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Future;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "결혼 목표 등록 요청")
public class GoalCreateRequestDTO {

    @NotBlank(message = "시안 제목은 필수입니다.")
    @Size(max = 50, message = "시안 제목은 50자 이하여야 합니다.")
    @ApiModelProperty(value = "시안 제목", example = "균형형 결혼 예산", required = true)
    private String name;

    @NotNull(message = "결혼식 날짜는 필수입니다.")
    @Future(message = "결혼식 날짜는 오늘 이후여야 합니다.")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "결혼식 날짜", example = "2027-10-09", required = true)
    private LocalDate weddingDate;

    @NotBlank(message = "결혼식 지역은 필수입니다.")
    @Size(max = 50, message = "결혼식 지역은 50자 이하여야 합니다.")
    @ApiModelProperty(value = "결혼식 지역", example = "서울(강남)", required = true)
    private String region;

    @NotNull(message = "신랑 분담 비율은 필수입니다.")
    @Min(value = 0, message = "신랑 분담 비율은 0 이상이어야 합니다.")
    @Max(value = 100, message = "신랑 분담 비율은 100 이하여야 합니다.")
    @ApiModelProperty(value = "신랑 분담 비율", example = "50", required = true)
    private Long groomRatio;

    @NotNull(message = "신부 분담 비율은 필수입니다.")
    @Min(value = 0, message = "신부 분담 비율은 0 이상이어야 합니다.")
    @Max(value = 100, message = "신부 분담 비율은 100 이하여야 합니다.")
    @ApiModelProperty(value = "신부 분담 비율", example = "50", required = true)
    private Long brideRatio;

    @Valid
    @NotEmpty(message = "목표 항목은 최소 1개 이상이어야 합니다.")
    @ApiModelProperty(value = "결혼 목표 항목 목록", required = true)
    private List<GoalItemCreateRequestDTO> items;
}
