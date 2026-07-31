package com.project.domain.onboarding.service;

import com.project.domain.onboarding.dto.OnboardingQueryRequestDTO;
import com.project.domain.onboarding.dto.OnboardingQueryResponseDTO;
import com.project.domain.onboarding.dto.OnboardingRegisterRequestDTO;
import com.project.domain.onboarding.dto.OnboardingRegisterResponseDTO;
import com.project.domain.onboarding.dto.OnboardingSelectRequestDTO;
import com.project.domain.onboarding.dto.OnboardingSelectResponseDTO;

public interface OnboardingService {

    // 1. 계좌/카드 등록 및 추가 API
    OnboardingRegisterResponseDTO registerOrAddAccount(Long userId, OnboardingRegisterRequestDTO requestDTO);

    // 2. 계좌/카드 목록 조회 API
    OnboardingQueryResponseDTO getAccountsOrCards(Long userId, OnboardingQueryRequestDTO requestDTO);

    // 3. 계좌/카드 선택 및 거래 내역 저장 API
    OnboardingSelectResponseDTO selectAccountsOrCards(Long userId, OnboardingSelectRequestDTO requestDTO);
}
