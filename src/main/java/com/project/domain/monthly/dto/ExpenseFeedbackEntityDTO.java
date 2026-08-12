package com.project.domain.monthly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseFeedbackEntityDTO {

    @Setter
    private Long feedbackId;
    private Long writerUserId;
    private Long receiverUserId;
    private Integer year;
    private Integer month;
    private String content;
}
