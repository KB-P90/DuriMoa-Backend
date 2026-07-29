package com.project.domain.calendar.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyResponseDTO {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private int count;
    private List<DailyRecordDTO> records;
}
