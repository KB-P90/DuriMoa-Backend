package com.project.domain.calendar.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.domain.calendar.domain.RecordType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyRecordDTO {
    private Long recordId;
    private RecordType type;
    private String category;
    private Long amount;
    private String memo;
    private String owner;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
