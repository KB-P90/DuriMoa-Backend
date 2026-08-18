package com.project.domain.onboarding.service;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.onboarding.dto.OnboardingWeddingFundRequestDTO;
import com.project.domain.onboarding.mapper.OnboardingMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OnboardingServiceImplTest {

    @Test
    void saveWeddingFundUpdatesAuthenticatedUserWeddingFund() {
        OnboardingMapper onboardingMapper = Mockito.mock(OnboardingMapper.class);
        OnboardingServiceImpl service = new OnboardingServiceImpl(null, onboardingMapper, null);
        OnboardingWeddingFundRequestDTO requestDTO = OnboardingWeddingFundRequestDTO.builder()
                .weddingFund("2000000")
                .build();

        Mockito.when(onboardingMapper.existsActiveUserById(1L)).thenReturn(true);
        Mockito.when(onboardingMapper.updateWeddingFund(1L, 2_000_000L)).thenReturn(1);

        service.saveWeddingFund(1L, requestDTO);

        Mockito.verify(onboardingMapper).existsActiveUserById(1L);
        Mockito.verify(onboardingMapper).updateWeddingFund(1L, 2_000_000L);
    }

    @Test
    void saveWeddingFundRejectsNegativeAmount() {
        OnboardingMapper onboardingMapper = Mockito.mock(OnboardingMapper.class);
        OnboardingServiceImpl service = new OnboardingServiceImpl(null, onboardingMapper, null);
        OnboardingWeddingFundRequestDTO requestDTO = OnboardingWeddingFundRequestDTO.builder()
                .weddingFund("-1")
                .build();

        CustomException exception = Assertions.assertThrows(
                CustomException.class,
                () -> service.saveWeddingFund(1L, requestDTO)
        );

        Assertions.assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        Mockito.verify(onboardingMapper, Mockito.never()).updateWeddingFund(Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    void saveWeddingFundAllowsSameAmountUpdateResult() {
        OnboardingMapper onboardingMapper = Mockito.mock(OnboardingMapper.class);
        OnboardingServiceImpl service = new OnboardingServiceImpl(null, onboardingMapper, null);
        OnboardingWeddingFundRequestDTO requestDTO = OnboardingWeddingFundRequestDTO.builder()
                .weddingFund("2000000")
                .build();

        Mockito.when(onboardingMapper.existsActiveUserById(1L)).thenReturn(true);
        Mockito.when(onboardingMapper.updateWeddingFund(1L, 2_000_000L)).thenReturn(0);

        service.saveWeddingFund(1L, requestDTO);

        Mockito.verify(onboardingMapper).updateWeddingFund(1L, 2_000_000L);
    }

    @Test
    void saveWeddingFundThrowsWhenUserDoesNotExist() {
        OnboardingMapper onboardingMapper = Mockito.mock(OnboardingMapper.class);
        OnboardingServiceImpl service = new OnboardingServiceImpl(null, onboardingMapper, null);
        OnboardingWeddingFundRequestDTO requestDTO = OnboardingWeddingFundRequestDTO.builder()
                .weddingFund("2000000")
                .build();

        Mockito.when(onboardingMapper.existsActiveUserById(99L)).thenReturn(false);

        CustomException exception = Assertions.assertThrows(
                CustomException.class,
                () -> service.saveWeddingFund(99L, requestDTO)
        );

        Assertions.assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        Mockito.verify(onboardingMapper, Mockito.never()).updateWeddingFund(Mockito.anyLong(), Mockito.anyLong());
    }
}
