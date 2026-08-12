package com.project.domain.monthly.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
@ApiModel(description = "지출 피드백 정보 DTO")
public class ExpenseFeedbackDTO {

    @ApiModelProperty(value = "지출 피드백 ID", example = "1")
    private Long feedbackId;

    @ApiModelProperty(value = "작성자 사용자 ID", example = "2")
    private Long writerUserId;

    @ApiModelProperty(value = "작성자 이름", example = "상대방")
    private String writerName;

    @JsonProperty("receiver_id")
    @ApiModelProperty(value = "수신자 사용자 ID", example = "2")
    private Long receiverId;

    @ApiModelProperty(value = "피드백 내용", example = "저번달보다 쇼핑을 줄였네 😍")
    private String content;
}
