package com.project.domain.calendar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecordResponseDTO {
    private Long recordId;

    public static RecordResponseDTO of(Long recordId) {
        return new RecordResponseDTO(recordId);
    }
}
