package com.project.domain.onboarding.controller;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.common.response.ApiResponse;
import com.project.domain.onboarding.dto.OnboardingQueryRequestDTO;
import com.project.domain.onboarding.dto.OnboardingQueryResponseDTO;
import com.project.domain.onboarding.dto.OnboardingRegisterRequestDTO;
import com.project.domain.onboarding.dto.OnboardingRegisterResponseDTO;
import com.project.domain.onboarding.dto.OnboardingSelectRequestDTO;
import com.project.domain.onboarding.dto.OnboardingSelectResponseDTO;
import com.project.domain.onboarding.service.OnboardingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/onboarding")
@Api(tags = "Onboarding", description = "온보딩 계좌/카드 연동 및 거래내역 저장 API")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @ApiOperation(
            value = "계좌/카드 등록 및 추가 API",
            notes = "입력받은 은행 또는 카드 계정 정보를 바탕으로, 사용자의 커넥티드 ID 존재 여부를 확인합니다.\n" +
                    "- 커넥티드 ID 미존재 시: CODEF 계정 등록 API(/v1/account/create) 호출\n" +
                    "- 커넥티드 ID 존재 시: CODEF 계정 추가 API(/v1/account/add) 호출\n" +
                    "등록/추가 완료 후 커넥티드 ID 및 등록된 금융기관 정보를 반환합니다."
    )
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<OnboardingRegisterResponseDTO>> registerOrAddAccount(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OnboardingRegisterRequestDTO requestDTO) {
        Long authenticatedUserId = resolveUserId(userId);
        log.info("온보딩 계좌/카드 등록 및 추가 요청 수신 - userId: {}", authenticatedUserId);
        OnboardingRegisterResponseDTO response = onboardingService.registerOrAddAccount(authenticatedUserId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("계정 등록/추가 성공", response));
    }

    @ApiOperation(
            value = "계좌/카드 목록 조회 API",
            notes = "입력받은 은행/카드 업무 구분(BK, CD) 및 기관 코드(organization)와 사용자의 커넥티드 ID를 바탕으로 특정 기관의 보유 계좌 번호 또는 카드 목록을 조회하여 반환합니다."
    )
    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<OnboardingQueryResponseDTO>> getAccountsOrCards(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OnboardingQueryRequestDTO requestDTO) {
        Long authenticatedUserId = resolveUserId(userId);
        log.info("온보딩 계좌/카드 목록 조회 요청 수신 - userId: {}, org: {}", authenticatedUserId, requestDTO.getOrganization());
        OnboardingQueryResponseDTO response = onboardingService.getAccountsOrCards(authenticatedUserId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("특정 기관 계좌/카드 목록 조회 성공", response));
    }

    @ApiOperation(
            value = "계좌/카드 선택 API",
            notes = "사용자가 선택한 특정 기관의 일부 계좌 번호 또는 카드 번호 목록을 입력받아, 해당 계좌/카드의 최근 거래 내역(승인 내역)만 수집하여 DB(account_record 또는 card_record)에 저장합니다."
    )
    @PostMapping("/select")
    public ResponseEntity<ApiResponse<OnboardingSelectResponseDTO>> selectAccountsOrCards(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OnboardingSelectRequestDTO requestDTO) {
        Long authenticatedUserId = resolveUserId(userId);
        log.info("온보딩 선택 계좌 거래 내역 동기화 요청 수신 - userId: {}, selectedNumbers: {}", authenticatedUserId, requestDTO.getSelectedNumbers());
        OnboardingSelectResponseDTO response = onboardingService.selectAccountsOrCards(authenticatedUserId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("선택 계좌 거래 내역 저장 완료", response));
    }

    private Long resolveUserId(Long userId) {
        if (userId != null && userId > 0) {
            return userId;
        }
        throw new CustomException(ErrorCode.UNAUTHORIZED, "인증 정보가 없거나 유효하지 않습니다. 로그인 후 이용해주세요.");
    }
}
