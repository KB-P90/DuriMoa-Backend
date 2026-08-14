package com.project.domain.transaction.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class CardRecord {
    private Long id;

    private Long cardId;
    private String sourceKey;

    private LocalDate usedDate;
    private LocalTime usedTime;

    private String storeName;
    private Long usedAmount;

    private String paymentType;
    private Integer installmentMonth;

    private String approvalNo;
    private LocalDate paymentDueDate;

    private String domesticForeignType;
    private String storeCorpNo;
    private String storeType;
    private String storeNo;

    private String cancelYn;
    private Long cancelAmount;

    private Long vat;
    private Long cashBack;

    private Long krwAmount;
    private String currency;

    private String rawData;
}
