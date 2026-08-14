package com.project.domain.transaction.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class AccountRecord {
    private Long accountId;
    private String sourceKey;
    private LocalDate transactionDate;
    private LocalTime transactionTime;
    private Long outAmount;
    private Long inAmount;
    private Long afterTransactionBalance;
    private String description1;
    private String description2;
    private String description3;
    private String description4;
    private String rawData;
}
