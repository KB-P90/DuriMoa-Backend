package com.project.domain.strategy.card.controller;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.common.response.ApiResponse;
import com.project.domain.strategy.card.dto.CardDetailResponseDTO;
import com.project.domain.strategy.card.dto.CardProductCollectionResultDTO;
import com.project.domain.strategy.card.dto.CardProductCrawlRequestDTO;
import com.project.domain.strategy.card.dto.CardRecommendationResponseDTO;
import com.project.domain.strategy.card.model.CardPaymentAmountValidator;
import com.project.domain.strategy.card.service.CardStrategyService;
import com.project.domain.strategy.card.service.collection.CardGorillaCardProductCollectionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "Card Strategy", description = "카드 추천 및 상세 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/strategy/card")
public class CardStrategyController {

    // 카드 추천과 상세 조회 비즈니스 로직을 담당하는 서비스입니다.
    private final CardStrategyService cardStrategyService;

    // 카드고릴라 상품 수집과 DB 반영을 담당하는 서비스입니다.
    private final CardGorillaCardProductCollectionService cardGorillaCardProductCollectionService;

    // 결제 예정 금액 기준으로 본인·상대방 카드 추천 결과를 조회합니다.
    @ApiOperation(value = "카드 추천 조회", notes = "결제 예정 금액 기준으로 활성 커플의 보유 카드를 비교합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<CardRecommendationResponseDTO>> recommendCards(
            @RequestParam(value = "amount", required = false) String amount,
            @ApiIgnore @AuthenticationPrincipal Long authenticatedUserId
    ) {
        Long paymentAmount = CardPaymentAmountValidator.validate(amount);
        CardRecommendationResponseDTO response = cardStrategyService.recommendCards(authenticatedUserId, paymentAmount);
        return ResponseEntity.ok(ApiResponse.success("카드 추천 조회에 성공했습니다.", response));
    }

    // 사용자 카드 식별자로 권한 검증 후 카드 상세 정보를 조회합니다.
    @ApiOperation(value = "카드 상세 조회", notes = "사용자 보유 카드 식별자 기준으로 카드 상품 상세 정보를 조회합니다.")
    @GetMapping("/{cardId}")
    public ResponseEntity<ApiResponse<CardDetailResponseDTO>> getCardDetail(
            @PathVariable("cardId") String cardIdText,
            @ApiIgnore @AuthenticationPrincipal Long authenticatedUserId
    ) {
        Long cardId = parseCardId(cardIdText);
        CardDetailResponseDTO response = cardStrategyService.getCardDetail(authenticatedUserId, cardId);
        return ResponseEntity.ok(ApiResponse.success("카드 상세 조회에 성공했습니다.", response));
    }

    // 카드고릴라 신용카드와 체크카드 상품을 수집해 카드 추천 DB에 반영합니다.
    @ApiOperation(value = "카드고릴라 카드 상품 수집", notes = "카드고릴라의 신용카드/체크카드 혜택 정보를 수집해 card_product에 등록 또는 갱신합니다.")
    @PostMapping("/products/collect/card-gorilla")
    public ResponseEntity<ApiResponse<CardProductCollectionResultDTO>> collectCardGorillaProducts(
            @Valid @RequestBody(required = false) CardProductCrawlRequestDTO request,
            @ApiIgnore @AuthenticationPrincipal Long authenticatedUserId
    ) {
        validateAuthenticatedUser(authenticatedUserId);
        Integer maxCount = request == null ? null : request.getMaxCount();
        String cardType = request == null ? null : request.getCardType();
        CardProductCollectionResultDTO response = cardGorillaCardProductCollectionService.collectCardProducts(maxCount, cardType);
        return ResponseEntity.ok(ApiResponse.success("카드고릴라 카드 상품 수집에 성공했습니다.", response));
    }

    // 상세 조회의 사용자 카드 식별자를 양수 Long으로 변환합니다.
    private Long parseCardId(String cardIdText) {
        return parsePositiveLong(
                cardIdText,
                new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "카드 정보를 찾을 수 없습니다.")
        );
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

    // DB를 변경하는 수집 요청은 인증된 사용자만 허용합니다.
    private void validateAuthenticatedUser(Long authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId <= 0) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }
}
