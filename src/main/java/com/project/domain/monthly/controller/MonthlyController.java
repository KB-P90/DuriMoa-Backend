package com.project.domain.monthly.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.monthly.dto.MonthlyExpenseResponseDTO;
import com.project.domain.monthly.service.MonthlyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Api(tags = "Monthly Expense", description = "월별 지출 관리 API")
@RestController
@RequestMapping("/api/expense/monthly")
@RequiredArgsConstructor
public class MonthlyController {

    private final MonthlyService monthlyService;

    @ApiOperation(
            value = "카테고리별 지출 조회",
            notes = "사용자 ID, 연도, 월을 입력받아 해당 월의 카테고리별 지출 금액, 전월 대비 비율, 지출 비율을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<MonthlyExpenseResponseDTO>> getMonthlyCategoryExpenses(
            // TODO: 추후 JWT 로그인 구현 완료 시 SecurityContext에서 userId를 추출하도록 변경 및 userId @RequestParam 삭제 예정
            @ApiParam(value = "사용자 ID (임시: JWT 적용 전)", example = "1")
            @RequestParam(value = "userId", required = false) Long userId,
            @ApiParam(value = "조회 연도", example = "2026")
            @RequestParam(value = "year", required = false) Integer year,
            @ApiParam(value = "조회 월", example = "7")
            @RequestParam(value = "month", required = false) Integer month
    ) {
        if (userId == null) {
            userId = 1L; // 임시 기본값 (JWT 로그인 적용 전 테스트용)
        }
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        if (month == null) {
            month = LocalDate.now().getMonthValue();
        }

        MonthlyExpenseResponseDTO response = monthlyService.getMonthlyCategoryExpenses(userId, year, month);
        return ResponseEntity.ok(ApiResponse.success("월별 카테고리별 지출 목록을 조회했습니다.", response));
    }
}
