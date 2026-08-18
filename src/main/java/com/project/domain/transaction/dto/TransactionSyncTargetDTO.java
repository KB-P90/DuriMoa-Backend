package com.project.domain.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSyncTargetDTO {
    private Long id;
    private String company;
    private String number;

    private boolean hasRecords;
}
