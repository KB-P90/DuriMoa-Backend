package com.project.domain.calendar.domain;

import com.project.common.enums.RecordType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRecord {
    private Long id;
    private Long userId;

    private Long cardRecordId;
    private Long accountRecordId;

    private LocalDate recordDate;

    private RecordType type;
    private Long amount;

    private String category;
    private String changedCategory;

    private String status;

    private String memo;
}
