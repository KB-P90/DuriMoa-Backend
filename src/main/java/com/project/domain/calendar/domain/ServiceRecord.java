package com.project.domain.calendar.domain;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class ServiceRecord {
    private Long id;

    private Long userId;

    private RecordType type;

    private String category;

    private String changedCategory;

    private Long amount;

    private LocalDate recordDate;

    private String memo;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
