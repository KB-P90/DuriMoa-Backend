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
@Api(tags = "Onboarding", description = "온보딩 API")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @ApiOperation(
            value = "계좌/카드 등록 및 추가 API",
            notes = "입력받은 금융기관 이름(KB국민은행, KB국민카드 등)과 아이디/비밀번호를 바탕으로 기관 코드와 규격을 자동 매핑하여 CODEF 계정 연동을 진행합니다.\n" +
                    "- 커넥티드 ID 미존재 시: CODEF 계정 등록 API(/v1/account/create) 호출\n" +
                    "- 커넥티드 ID 존재 시: CODEF 계정 추가 API(/v1/account/add) 호출\n" +
                    "등록 완료 후 커넥티드 ID 및 금융기관 정보 1건을 반환 및 DB에 저장합니다."
    )
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<OnboardingRegisterResponseDTO>> registerOrAddAccount(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OnboardingRegisterRequestDTO requestDTO) {
        if (userId == null || userId <= 0) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "인증 정보가 없거나 유효하지 않습니다. 로그인 후 이용해주세요.");
        }
        log.info("온보딩 계좌/카드 등록 요청 수신 - userId: {}, company: {}", userId, requestDTO.getCompany());
        OnboardingRegisterResponseDTO response = onboardingService.registerOrAddAccount(userId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("계정 등록/추가 성공", response));
    }

    @ApiOperation(
            value = "계좌/카드 목록 조회 API",
            notes = "입력받은 금융기관 이름(KB국민은행, KB국민카드 등)만 전달받아 백엔드에서 사용자 커넥티드 ID 및 기관 코드를 자동 매핑하여 보유 계좌/카드 목록을 조회합니다."
    )
    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<OnboardingQueryResponseDTO>> getAccountsOrCards(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OnboardingQueryRequestDTO requestDTO) {
        if (userId == null || userId <= 0) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "인증 정보가 없거나 유효하지 않습니다. 로그인 후 이용해주세요.");
        }
        log.info("온보딩 계좌/카드 목록 조회 요청 수신 - userId: {}, company: {}", userId, requestDTO.getCompany());
        OnboardingQueryResponseDTO response = onboardingService.getAccountsOrCards(userId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("특정 기관 계좌/카드 목록 조회 성공", response));
    }

    @ApiOperation(
            value = "계좌/카드 선택 API",
            notes = "사용자가 선택한 금융기관 이름과 계좌/카드 번호 목록을 전달받아, 사용자 커넥티드 ID로 거래 내역을 수집하고 DB에 저장합니다."
    )
    @PostMapping("/select")
    public ResponseEntity<ApiResponse<OnboardingSelectResponseDTO>> selectAccountsOrCards(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OnboardingSelectRequestDTO requestDTO) {
        if (userId == null || userId <= 0) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "인증 정보가 없거나 유효하지 않습니다. 로그인 후 이용해주세요.");
        }
        log.info("온보딩 선택 계좌 거래 내역 동기화 요청 수신 - userId: {}, company: {}, selectedNumbers: {}",
                userId, requestDTO.getCompany(), requestDTO.getSelectedNumbers());
        OnboardingSelectResponseDTO response = onboardingService.selectAccountsOrCards(userId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("선택 계좌 거래 내역 저장 완료", response));
    }
}
