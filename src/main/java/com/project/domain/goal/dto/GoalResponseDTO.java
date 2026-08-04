package com.project.domain.goal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.domain.goal.domain.GoalVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "결혼 시안 조회 응답")
public class GoalResponseDTO {

    @ApiModelProperty(value = "목표 ID", example = "1")
    private Long goalId;

    @ApiModelProperty(value = "시안 제목", example = "균형형 결혼 예산")
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "결혼식 날짜", example = "2027-10-09")
    private LocalDate weddingDate;

    @ApiModelProperty(value = "결혼식 지역", example = "서울(강남)")
    private String region;

    @ApiModelProperty(value = "총예산", example = "23200000")
    private Long totalBudget;

    @ApiModelProperty(value = "신랑 분담 비율", example = "50")
    private Long groomRatio;

    @ApiModelProperty(value = "신부 분담 비율", example = "50")
    private Long brideRatio;

    @ApiModelProperty(value = "목표 상태", allowableValues = "NORMAL,PENDING,CONFIRMED",
            example = "NORMAL")
    private String status;

    @ApiModelProperty(value = "메인 목표 여부", example = "false")
    private Boolean isMain;

    @ApiModelProperty(value = "현재 사용자의 메인 시안 요청 여부", example = "false")
    private Boolean requestedByMe;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "메인 시안 승인 요청 일시", example = "2026-07-31 14:00:00")
    private LocalDateTime requestedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "생성 일시", example = "2026-07-30 10:30:00")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "목표 항목 목록")
    private List<GoalItemResponseDTO> items;

    public static GoalResponseDTO from(
            GoalVO goal,
            List<GoalItemResponseDTO> items,
            Long authenticatedUserId
    ) {
        Boolean requestedByMe = "PENDING".equals(goal.getStatus())
                && authenticatedUserId.equals(goal.getRequestedBy());
        return GoalResponseDTO.builder()
                .goalId(goal.getId())
                .name(goal.getName())
                .weddingDate(goal.getWeddingDate())
                .region(goal.getRegion())
                .totalBudget(goal.getTotalBudget())
                .groomRatio(goal.getGroomRatio())
                .brideRatio(goal.getBrideRatio())
                .status(goal.getStatus())
                .isMain(goal.getIsMain())
                .requestedByMe(requestedByMe)
                .requestedAt(goal.getRequestedAt())
                .createdAt(goal.getCreatedAt())
                .items(items)
                .build();
    }
}
