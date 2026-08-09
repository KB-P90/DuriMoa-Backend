package com.project.domain.progress.service;

import com.project.domain.progress.dto.ProgressResponseDTO;
import com.project.domain.progress.dto.MonthlyProgressResponseDTO;
import com.project.domain.progress.dto.ProgressItemCompletionRequestDTO;
import com.project.domain.progress.dto.ProgressItemCompletionResponseDTO;

public interface ProgressService {

    ProgressResponseDTO getProgress(Long userId);

    MonthlyProgressResponseDTO getMonthlyProgress(Long userId);

    ProgressItemCompletionResponseDTO updateItemCompletion(
            Long userId,
            Long goalItemId,
            ProgressItemCompletionRequestDTO request
    );
}
