package com.project.domain.onboarding;

import com.project.domain.codef.dto.CodefApiResponseDTO;
import com.project.domain.codef.service.CodefService;
import com.project.domain.onboarding.dto.OnboardingQueryRequestDTO;
import com.project.domain.onboarding.dto.OnboardingQueryResponseDTO;
import com.project.domain.onboarding.dto.OnboardingRegisterRequestDTO;
import com.project.domain.onboarding.dto.OnboardingRegisterResponseDTO;
import com.project.domain.onboarding.dto.OnboardingSelectRequestDTO;
import com.project.domain.onboarding.dto.OnboardingSelectResponseDTO;
import com.project.domain.onboarding.mapper.OnboardingMapper;
import com.project.domain.onboarding.service.OnboardingServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.common.config.CodefConfig;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private CodefService codefService;

    @Mock
    private OnboardingMapper onboardingMapper;

    @Mock
    private CodefConfig codefConfig;

    @InjectMocks
    private OnboardingServiceImpl onboardingService;

    @Test
    @DisplayName("커넥티드 ID 미존재 시 계정 등록 API 호출 및 생성 처리 테스트")
    void registerOrAddAccount_CreatesNewConnectedId_WhenNotExists() {
        // given
        Long userId = 1L;
        OnboardingRegisterRequestDTO requestDTO = OnboardingRegisterRequestDTO.builder()
                .company("KB국민은행")
                .id("test_user_id")
                .password("password123!")
                .build();

        when(onboardingMapper.findConnectedIdByUserId(userId)).thenReturn(null);
        when(codefService.encryptPassword(any())).thenReturn("encrypted_pw");

        com.project.domain.codef.dto.CodefConnectedIdResponseDTO idResponse = com.project.domain.codef.dto.CodefConnectedIdResponseDTO.builder()
                .connectedId("new_demo_connected_id")
                .build();
        CodefApiResponseDTO<com.project.domain.codef.dto.CodefConnectedIdResponseDTO> codefRes = CodefApiResponseDTO.<com.project.domain.codef.dto.CodefConnectedIdResponseDTO>builder()
                .result(com.project.domain.codef.dto.CodefResultDTO.builder().code("CF-00000").message("성공").build())
                .data(idResponse)
                .build();
        when(codefService.createConnectedId(any())).thenReturn(codefRes);

        // when
        OnboardingRegisterResponseDTO result = onboardingService.registerOrAddAccount(userId, requestDTO);

        // then
        assertNotNull(result);
        assertEquals("CREATED", result.getActionType());
        assertEquals("new_demo_connected_id", result.getConnectedId());
        assertEquals("KB국민은행", result.getCompany());
        verify(onboardingMapper).updateUserConnectedId(userId, "new_demo_connected_id");
    }

    @Test
    @DisplayName("커넥티드 ID 기존 존재 시 계정 추가 API 호출 및 추가 처리 테스트")
    void registerOrAddAccount_AddsAccount_WhenConnectedIdExists() {
        // given
        Long userId = 1L;
        OnboardingRegisterRequestDTO requestDTO = OnboardingRegisterRequestDTO.builder()
                .company("KB국민카드")
                .id("test_user_id")
                .password("password123!")
                .build();

        when(onboardingMapper.findConnectedIdByUserId(userId)).thenReturn("existing_connected_id");

        // when
        OnboardingRegisterResponseDTO result = onboardingService.registerOrAddAccount(userId, requestDTO);

        // then
        assertNotNull(result);
        assertEquals("ADDED", result.getActionType());
        assertEquals("existing_connected_id", result.getConnectedId());
        assertEquals("KB국민카드", result.getCompany());
        verify(codefService).addConnectedAccount(any());
    }

    @Test
    @DisplayName("계좌 목록 조회 성공 테스트")
    void getAccountsOrCards_Success() {
        // given
        Long userId = 1L;
        OnboardingQueryRequestDTO requestDTO = OnboardingQueryRequestDTO.builder()
                .company("KB국민은행")
                .build();

        CodefApiResponseDTO<Object> codefRes = CodefApiResponseDTO.builder()
                .result(com.project.domain.codef.dto.CodefResultDTO.builder().code("CF-00000").message("성공").build())
                .data(Collections.singletonList(Collections.singletonMap("resAccount", "1234567890")))
                .build();

        when(onboardingMapper.findConnectedIdByUserId(userId)).thenReturn("demo_connected_id");
        when(codefService.getAccountList(any())).thenReturn(codefRes);

        // when
        OnboardingQueryResponseDTO result = onboardingService.getAccountsOrCards(userId, requestDTO);

        // then
        assertNotNull(result);
        assertEquals("demo_connected_id", result.getConnectedId());
        assertEquals("BK", result.getBusinessType());
        assertNotNull(result.getItemList());
    }

    @Test
    @DisplayName("선택 계좌 거래 내역 수집 및 DB 저장 동기화 테스트")
    void selectAccountsOrCards_Success() {
        // given
        Long userId = 1L;
        OnboardingSelectRequestDTO requestDTO = OnboardingSelectRequestDTO.builder()
                .company("KB국민은행")
                .selectedNumbers(Collections.singletonList("12345678901234"))
                .startDate("20260701")
                .endDate("20260731")
                .build();

        when(onboardingMapper.findConnectedIdByUserId(userId)).thenReturn("demo_connected_id");
        when(onboardingMapper.findAccountIdByUserAndNumber(userId, "12345678901234", "KB국민은행")).thenReturn(10L);

        com.project.domain.codef.dto.CodefBankTransactionItemDTO item = com.project.domain.codef.dto.CodefBankTransactionItemDTO.builder()
                .transactionDate("20260715")
                .outAmount(50000L)
                .description1("식비 지출")
                .build();

        com.project.domain.codef.dto.CodefBankTransactionResponseDTO bankResponseData = com.project.domain.codef.dto.CodefBankTransactionResponseDTO.builder()
                .items(Collections.singletonList(item))
                .build();

        CodefApiResponseDTO<com.project.domain.codef.dto.CodefBankTransactionResponseDTO> codefRes = CodefApiResponseDTO.<com.project.domain.codef.dto.CodefBankTransactionResponseDTO>builder()
                .result(com.project.domain.codef.dto.CodefResultDTO.builder().code("CF-00000").message("성공").build())
                .data(bankResponseData)
                .build();
        when(codefService.getBankTransactionList(any())).thenReturn(codefRes);

        // when
        OnboardingSelectResponseDTO result = onboardingService.selectAccountsOrCards(userId, requestDTO);

        // then
        assertNotNull(result);
        assertEquals("BK", result.getBusinessType());
        assertEquals(1, result.getTotalSavedRecords());
        assertEquals(1, result.getProcessedNumbers().size());
        verify(onboardingMapper).insertAccountRecord(any());
    }
}
