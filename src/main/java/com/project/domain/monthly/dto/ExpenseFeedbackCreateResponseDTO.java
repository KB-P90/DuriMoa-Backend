package com.project.domain.monthly.dto;

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
@ApiModel(description = "월별 지출 피드백 생성 응답 DTO")
public class ExpenseFeedbackCreateResponseDTO {

    @ApiModelProperty(value = "피드백 ID", example = "3")
    private Long feedbackId;

    @ApiModelProperty(value = "작성자 사용자 ID", example = "1")
    private Long writerUserId;

    @ApiModelProperty(value = "작성자 이름", example = "나")
    private String writerName;

    @ApiModelProperty(value = "수신자 사용자 ID", example = "2")
    private Long receiverUserId;

    @ApiModelProperty(value = "수신자 이름", example = "상대방")
    private String receiverName;

    @ApiModelProperty(value = "피드백 대상 연도", example = "2026")
    private Integer year;

    @ApiModelProperty(value = "피드백 대상 월", example = "7")
    private Integer month;

    @ApiModelProperty(value = "피드백 내용", example = "우리 다음달에는 좀 더 아껴보자!!")
    private String content;
}
