package com.project.domain.progress.service;

import com.project.domain.progress.dto.ProgressResponseDTO;

public interface ProgressService {

    ProgressResponseDTO getProgress(Long userId);
}
