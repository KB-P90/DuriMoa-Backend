package com.project.domain.strategy.card.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.strategy.card.dto.CardDetailResponseDTO;
import com.project.domain.strategy.card.dto.CardRecommendationResponseDTO;
import com.project.domain.strategy.card.exception.CardStrategyException.CardAuthenticationRequiredException;
import com.project.domain.strategy.card.exception.CardStrategyException.CardNotFoundException;
import com.project.domain.strategy.card.model.CardPaymentAmountValidator;
import com.project.domain.strategy.card.service.CardStrategyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "Card Strategy", description = "카드 추천 및 상세 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/strategy/card")
public class CardStrategyController {

    // 카드 추천과 상세 조회 비즈니스 로직을 담당하는 서비스입니다.
    private final CardStrategyService cardStrategyService;

    // 결제 예정 금액 기준으로 본인·상대방 카드 추천 결과를 조회합니다.
    @ApiOperation(value = "카드 추천 조회", notes = "결제 예정 금액 기준으로 활성 커플의 보유 카드를 비교합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<CardRecommendationResponseDTO>> recommendCards(
            @RequestParam(value = "amount", required = false) String amount,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            Authentication authentication
    ) {
        Long paymentAmount = CardPaymentAmountValidator.validate(amount);
        Long authenticatedUserId = resolveAuthenticatedUserId(authentication, userIdHeader);
        CardRecommendationResponseDTO response = cardStrategyService.recommendCards(authenticatedUserId, paymentAmount);
        return ResponseEntity.ok(ApiResponse.success("카드 추천 조회에 성공했습니다.", response));
    }

    // 사용자 카드 식별자로 권한 검증 후 카드 상세 정보를 조회합니다.
    @ApiOperation(value = "카드 상세 조회", notes = "사용자 보유 카드 식별자 기준으로 카드 상품 상세 정보를 조회합니다.")
    @GetMapping("/{userCardKey}")
    public ResponseEntity<ApiResponse<CardDetailResponseDTO>> getCardDetail(
            @PathVariable("userCardKey") String userCardKeyText,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            Authentication authentication
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(authentication, userIdHeader);
        Long userCardKey = parseUserCardKey(userCardKeyText);
        CardDetailResponseDTO response = cardStrategyService.getCardDetail(authenticatedUserId, userCardKey);
        return ResponseEntity.ok(ApiResponse.success("카드 상세 조회에 성공했습니다.", response));
    }

    // Spring Security 인증 정보 또는 개발용 헤더에서 사용자 ID를 추출합니다.
    private Long resolveAuthenticatedUserId(Authentication authentication, String userIdHeader) {
        Long authenticationUserId = parseAuthenticationName(authentication);
        if (authenticationUserId != null) {
            return authenticationUserId;
        }
        if (userIdHeader != null && !userIdHeader.trim().isEmpty()) {
            return parsePositiveLong(userIdHeader, new CardAuthenticationRequiredException());
        }
        throw new CardAuthenticationRequiredException();
    }

    // Authentication 이름이 숫자 사용자 ID일 때 Long으로 변환합니다.
    private Long parseAuthenticationName(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String principalName = authentication.getName();
        if (principalName == null || "anonymousUser".equals(principalName)) {
            return null;
        }
        try {
            Long userId = Long.valueOf(principalName);
            return userId > 0 ? userId : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    // 상세 조회의 사용자 카드 식별자를 양수 Long으로 변환합니다.
    private Long parseUserCardKey(String userCardKeyText) {
        return parsePositiveLong(userCardKeyText, new CardNotFoundException("카드 정보를 찾을 수 없습니다."));
    }

    // 숫자 문자열을 양수 Long으로 변환하고 실패 시 지정된 예외를 던집니다.
    private Long parsePositiveLong(String value, RuntimeException exception) {
        String normalizedValue = value == null ? "" : value.trim();
        if (!normalizedValue.matches("\\d+")) {
            throw exception;
        }
        try {
            Long parsedValue = Long.valueOf(normalizedValue);
            if (parsedValue <= 0) {
                throw exception;
            }
            return parsedValue;
        } catch (NumberFormatException numberFormatException) {
            throw exception;
        }
    }
}
