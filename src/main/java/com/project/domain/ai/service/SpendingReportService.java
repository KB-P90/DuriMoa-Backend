package com.project.domain.ai.service;

import com.project.domain.ai.dto.SpendingReportResponseDTO;

public interface SpendingReportService {

    SpendingReportResponseDTO generate(Long userId);
}
