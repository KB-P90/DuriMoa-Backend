package com.project.domain.progress.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.domain.progress.dto.ProgressResponseDTO;
import com.project.domain.progress.exception.ProgressExceptionHandler;
import com.project.domain.progress.exception.SavingRecordNotFoundException;
import com.project.domain.progress.service.ProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ProgressControllerTest {

    @Mock
    private ProgressService progressService;

    @InjectMocks
    private ProgressController progressController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(progressController)
                .setControllerAdvice(new ProgressExceptionHandler())
                .build();
    }

    @Test
    void returnsProgressInApiResponse() throws Exception {
        ProgressResponseDTO response = ProgressResponseDTO.builder()
                .totalSavingAmount(1_250_000L)
                .targetAmount(50_000_000L)
                .progressRate(2)
                .groomSavingAmount(500_000L)
                .brideSavingAmount(750_000L)
                .build();
        when(progressService.getProgress()).thenReturn(response);

        mockMvc.perform(get("/api/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("현황을 성공적으로 조회했습니다."))
                .andExpect(jsonPath("$.data.totalSavingAmount").value(1_250_000))
                .andExpect(jsonPath("$.data.targetAmount").value(50_000_000))
                .andExpect(jsonPath("$.data.progressRate").value(2));
    }

    @Test
    void returnsApiResponseForSavingRecordNotFoundException() throws Exception {
        when(progressService.getProgress()).thenThrow(new SavingRecordNotFoundException());

        mockMvc.perform(get("/api/progress"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("저축 거래 내역이 없습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
