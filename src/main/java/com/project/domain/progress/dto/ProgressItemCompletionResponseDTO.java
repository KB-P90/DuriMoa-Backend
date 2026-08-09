package com.project.domain.progress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressItemCompletionResponseDTO {

    private Long goalItemId;
    private Boolean completed;
    private Integer completedItemCount;
    private Integer totalItemCount;
}
