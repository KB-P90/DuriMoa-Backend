package com.project.domain.monthly.controller;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.common.response.ApiResponse;
import com.project.domain.monthly.dto.MonthlyExpenseResponseDTO;
import com.project.domain.monthly.dto.SavingMissionRecommendationRequestDTO;
import com.project.domain.monthly.dto.SavingMissionRecommendationResponseDTO;
import com.project.domain.monthly.dto.SavingMissionResponseDTO;
import com.project.domain.monthly.service.MonthlyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.time.LocalDate;

@Api(tags = "Monthly Expense", description = "월별 지출 관리 API")
@RestController
@RequestMapping("/api/expense")
@RequiredArgsConstructor
public class MonthlyController {

    private final MonthlyService monthlyService;

    @ApiOperation(
            value = "월별 카테고리별 지출 및 피드백 조회",
            notes = "인증 사용자 기준, 연도, 월을 입력받아 해당 월의 본인 및 상대방의 카테고리별 지출 금액, 소득 대비 지출 비율 및 지출 피드백을 조회합니다."
    )
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<MonthlyExpenseResponseDTO>> getMonthlyCategoryExpenses(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication,
            @ApiParam(value = "조회 연도", example = "2026")
            @RequestParam(value = "year", required = false) Integer year,
            @ApiParam(value = "조회 월", example = "7")
            @RequestParam(value = "month", required = false) Integer month
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        if (month == null) {
            month = LocalDate.now().getMonthValue();
        }

        MonthlyExpenseResponseDTO response = monthlyService.getMonthlyCategoryExpenses(authenticatedUserId, year, month);
        return ResponseEntity.ok(ApiResponse.success("월별 카테고리별 지출 및 피드백을 조회했습니다.", response));
    }

    @ApiOperation(
            value = "월별 절약 미션 조회",
            notes = "연도와 월을 입력받아 해당 월의 절약 미션 목록, 총 예상 절감 금액, 미션 도전 가능 여부(isSelectable)를 조회합니다."
    )
    @GetMapping("/saving-missions")
    public ResponseEntity<ApiResponse<SavingMissionResponseDTO>> getMonthlySavingMissions(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication,
            @ApiParam(value = "조회 연도", example = "2026")
            @RequestParam(value = "year", required = false) Integer year,
            @ApiParam(value = "조회 월", example = "7")
            @RequestParam(value = "month", required = false) Integer month
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        if (month == null) {
            month = LocalDate.now().getMonthValue();
        }

        SavingMissionResponseDTO response = monthlyService.getMonthlySavingMissions(authenticatedUserId, year, month);
        return ResponseEntity.ok(ApiResponse.success("월별 절약 미션 목록을 조회했습니다.", response));
    }

    @ApiOperation(
            value = "절약 미션 도전",
            notes = "미션 ID를 입력받아 절약 미션 상태를 '도전하기'에서 '도전중'으로 변경합니다."
    )
    @PostMapping("/saving-missions/{missionId}")
    public ResponseEntity<ApiResponse<Void>> startSavingMission(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication,
            @ApiParam(value = "절약 미션 ID", example = "101", required = true)
            @PathVariable("missionId") Long missionId
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
        monthlyService.startSavingMission(authenticatedUserId, missionId);
        return ResponseEntity.ok(ApiResponse.success("절약 미션 도전을 시작했습니다."));
    }

    @ApiOperation(
            value = "절약 추천 미션 생성",
            notes = "직전 월 소비 패턴을 분석하여 절약이 가능한 카테고리를 선정하고 추천 미션을 생성합니다."
    )
    @PostMapping("/saving-missions/recommendations")
    public ResponseEntity<ApiResponse<SavingMissionRecommendationResponseDTO>> generateSavingMissionRecommendations(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication,
            @RequestBody SavingMissionRecommendationRequestDTO requestDTO
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
        if (requestDTO != null) {
            requestDTO.setUserId(authenticatedUserId);
        }
        return monthlyService.generateSavingMissionRecommendations(requestDTO);
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

