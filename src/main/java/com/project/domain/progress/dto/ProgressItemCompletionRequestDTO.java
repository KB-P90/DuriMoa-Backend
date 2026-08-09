package com.project.domain.progress.dto;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProgressItemCompletionRequestDTO {

    @NotNull(message = "completed 값은 필수입니다.")
    private Boolean completed;
}
