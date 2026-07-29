package com.project.domain.progress.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.project.domain.progress.dto.ProgressResponseDTO;
import org.junit.jupiter.api.Test;

class LocalMockProgressServiceTest {

    private final LocalMockProgressService progressService = new LocalMockProgressService();

    @Test
    void returnsMockDataForAllProgressScreens() {
        ProgressResponseDTO response = progressService.getProgress();

        assertEquals(31_100_000L, response.getTotalSavingAmount());
        assertEquals(50_000_000L, response.getTargetAmount());
        assertEquals(62, response.getProgressRate());
        assertEquals(5, response.getGoalChecklist().size());
        assertEquals(2, response.getIndividualProgress().size());
        assertEquals(6, response.getMonthlySuccessRecords().size());
        assertTrue(response.getGoalChecklist().get(0).getCompleted());
    }
}
