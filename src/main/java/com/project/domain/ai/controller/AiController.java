package com.project.domain.ai.controller;

import com.project.common.response.ApiResponse;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.ai.dto.AiChatRequestDTO;
import com.project.domain.ai.dto.AiChatResponseDTO;
import com.project.domain.ai.dto.AiConnectionTestResponseDTO;
import com.project.domain.ai.dto.WeddingBudgetRecommendationRequestDTO;
import com.project.domain.ai.dto.WeddingBudgetRecommendationResponseDTO;
import com.project.domain.ai.service.AiService;
import com.project.domain.ai.service.WeddingBudgetRecommendationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
@Api(tags = "AI", description = "AI API")
public class AiController {

    private final AiService aiService;
    private final WeddingBudgetRecommendationService weddingBudgetRecommendationService;

    @PostMapping("/test")
    @ApiOperation(value = "OpenAI 연결 테스트")
    public ResponseEntity<ApiResponse<AiConnectionTestResponseDTO>> testConnection() {
        AiConnectionTestResponseDTO result = aiService.testConnection();
        return ResponseEntity.ok(ApiResponse.success("OpenAI 연결 테스트에 성공했습니다.", result));
    }

    @PostMapping("/chat")
    @ApiOperation(value = "AI 채팅 메시지 전송")
    public ResponseEntity<ApiResponse<AiChatResponseDTO>> chat(
            @Valid @RequestBody AiChatRequestDTO requestDTO
    ) {
        AiChatResponseDTO result = aiService.chat(requestDTO.getMessage());
        return ResponseEntity.ok(ApiResponse.success("AI 응답을 생성했습니다.", result));
    }

    @PostMapping("/wedding-budget/recommendation")
    @ApiOperation(
            value = "소비 내역 기반 AI 결혼 예산 추천",
            notes = "결혼 예정 월에 따라 예식장과 스드메의 시기별 가격 가중치를 구분해 적용합니다."
    )
    public ResponseEntity<ApiResponse<WeddingBudgetRecommendationResponseDTO>>
            recommendWeddingBudget(
                    @ApiIgnore @AuthenticationPrincipal Long userId,
                    @ApiIgnore Authentication authentication,
                    @Valid @RequestBody WeddingBudgetRecommendationRequestDTO requestDTO
            ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
        WeddingBudgetRecommendationResponseDTO result =
                weddingBudgetRecommendationService.recommend(authenticatedUserId, requestDTO);
        return ResponseEntity.ok(
                ApiResponse.success("AI 결혼 예산 추천을 생성했습니다.", result)
        );
    }

    private Long resolveAuthenticatedUserId(Long userId, Authentication authentication) {
        if (userId != null && userId > 0) {
            return userId;
        }
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Number) {
                Long authenticationUserId = ((Number) principal).longValue();
                if (authenticationUserId > 0) {
                    return authenticationUserId;
                }
            }
        }
        throw new CustomException(ErrorCode.UNAUTHORIZED);
    }
}
