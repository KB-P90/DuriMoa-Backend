package com.project.domain.transaction.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class CardTransactionRecordDTO {
    private Long cardId;
    private String sourceKey;
    private LocalDate usedDate;
    private LocalTime usedTime;
    private String storeName;
    private Long usedAmount;
    private String cancelYn;
    private Long cancelAmount;
    private String rawData;
}
