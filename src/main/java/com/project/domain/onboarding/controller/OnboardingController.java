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
import com.project.domain.onboarding.dto.OnboardingWeddingFundRequestDTO;
import com.project.domain.onboarding.service.OnboardingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;
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
            notes = "입력받은 금융기관 이름과 아이디/비밀번호를 바탕으로 기관 코드와 규격을 자동 매핑하여 CODEF 계정 연동을 진행합니다.\n" +
                    "- 커넥티드 ID 미존재 시: CODEF 계정 등록 API(/v1/account/create) 호출\n" +
                    "- 커넥티드 ID 존재 시: CODEF 계정 추가 API(/v1/account/add) 호출\n\n" +
                    "📌 [주요 지원 금융기관 명칭 (company)]\n" +
                    "• 은행: 산업은행, 기업은행, 국민은행, 수협은행, 농협은행, 우리은행, SC은행, 씨티은행, 대구은행, 부산은행, 광주은행, 제주은행, 전북은행, 경남은행, 새마을금고, 신협은행, 우체국, KEB하나은행, 신한은행, K뱅크\n" +
                    "• 카드: KB카드, 현대카드, 삼성카드, NH카드, BC카드, 신한카드, 씨티카드, 우리카드, 롯데카드, 하나카드, 전북카드, 광주카드, 수협카드, 제주카드"
    )
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<OnboardingRegisterResponseDTO>> registerOrAddInstitution(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OnboardingRegisterRequestDTO requestDTO) {
        if (userId == null || userId <= 0) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "인증 정보가 없거나 유효하지 않습니다. 로그인 후 이용해주세요.");
        }
        log.info("온보딩 계좌/카드 등록 요청 수신 - userId: {}, company: {}", userId, requestDTO.getCompany());
        OnboardingRegisterResponseDTO response = onboardingService.registerOrAddInstitution(userId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("계정 등록/추가 성공", response));
    }

    @ApiOperation(
            value = "계좌/카드 목록 조회 API",
            notes = "입력받은 금융기관 이름을 파라미터로 전달받아 백엔드에서 사용자 커넥티드 ID 및 기관 코드를 자동 매핑하여 보유 계좌/카드 목록을 조회합니다.\n\n" +
                    "📌 [주요 지원 금융기관 명칭 (company)]\n" +
                    "• 은행: 산업은행, 기업은행, 국민은행, 수협은행, 농협은행, 우리은행, SC은행, 씨티은행, 대구은행, 부산은행, 광주은행, 제주은행, 전북은행, 경남은행, 새마을금고, 신협은행, 우체국, KEB하나은행, 신한은행, K뱅크\n" +
                    "• 카드: KB카드, 현대카드, 삼성카드, NH카드, BC카드, 신한카드, 씨티카드, 우리카드, 롯데카드, 하나카드, 전북카드, 광주카드, 수협카드, 제주카드"
    )
    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<OnboardingQueryResponseDTO>> getAccountsOrCards(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam(value = "금융기관 이름 (예: 국민은행, KB카드)", required = true, example = "국민은행")
            @RequestParam("company") String company) {
        if (userId == null || userId <= 0) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "인증 정보가 없거나 유효하지 않습니다. 로그인 후 이용해주세요.");
        }
        if (!StringUtils.hasText(company)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "금융기관 이름(company)은 필수입니다.");
        }
        log.info("온보딩 계좌/카드 목록 조회 요청 수신 - userId: {}, company: {}", userId, company);
        OnboardingQueryRequestDTO requestDTO = OnboardingQueryRequestDTO.builder().company(company).build();
        OnboardingQueryResponseDTO response = onboardingService.getAccountsOrCards(userId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("특정 기관 계좌/카드 목록 조회 성공", response));
    }

    @ApiOperation(
            value = "계좌/카드 선택 API",
            notes = "사용자가 선택한 금융기관 이름과 계좌/카드 번호 목록을 전달받아, 사용자 커넥티드 ID로 거래 내역을 수집하고 DB에 저장합니다.\n\n" +
                    "📌 [주요 지원 금융기관 명칭 (company)]\n" +
                    "• 은행: 산업은행, 기업은행, 국민은행, 수협은행, 농협은행, 우리은행, SC은행, 씨티은행, 대구은행, 부산은행, 광주은행, 제주은행, 전북은행, 경남은행, 새마을금고, 신협은행, 우체국, KEB하나은행, 신한은행, K뱅크\n" +
                    "• 카드: KB카드, 현대카드, 삼성카드, NH카드, BC카드, 신한카드, 씨티카드, 우리카드, 롯데카드, 하나카드, 전북카드, 광주카드, 수협카드, 제주카드"
    )
    @PostMapping("/select")
    public ResponseEntity<ApiResponse<OnboardingSelectResponseDTO>> selectAccountsOrCards(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OnboardingSelectRequestDTO requestDTO) {
        if (userId == null || userId <= 0) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "인증 정보가 없거나 유효하지 않습니다. 로그인 후 이용해주세요.");
        }
        log.info("온보딩 선택 계좌 거래 내역 동기화 요청 수신 - userId: {}, company: {}, selectedItems: {}",
                userId, requestDTO.getCompany(), requestDTO.getSelectedItems());
        OnboardingSelectResponseDTO response = onboardingService.selectAccountsOrCards(userId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("선택 계좌 거래 내역 저장 완료", response));
    }

    @ApiOperation(
            value = "결혼 자금 입력 API",
            notes = "온보딩에서 사용자가 결혼 자금으로 포함할 현재 보유 금액을 입력합니다. 입력된 금액은 홈 자산 요약과 온보딩 체크리스트 판단에 사용됩니다."
    )
    @PostMapping("/wedding-fund")
    public ResponseEntity<ApiResponse<Void>> saveWeddingFund(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OnboardingWeddingFundRequestDTO requestDTO) {
        if (userId == null || userId <= 0) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "인증 정보가 없거나 유효하지 않습니다. 로그인 후 이용해주세요.");
        }
        log.info("온보딩 결혼 자금 입력 요청 수신 - userId: {}", userId);
        onboardingService.saveWeddingFund(userId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("결혼 자금 입력이 완료되었습니다."));
    }
}
