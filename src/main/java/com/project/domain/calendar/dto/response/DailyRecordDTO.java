package com.project.domain.calendar.dto.response;

import com.project.common.enums.RecordType;
import lombok.*;

@Getter
@Setter
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
}
