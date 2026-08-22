package com.project.domain.transaction.controller;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.common.response.ApiResponse;
import com.project.domain.monthly.service.MonthlyService;
import com.project.domain.transaction.dto.TransactionSyncResponseDTO;
import com.project.domain.transaction.service.TransactionSyncService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * 매일 00시 배치(DailyTransactionSyncScheduler)와 동일한 로직을
 * 인증 사용자 1명 기준으로 즉시 실행하기 위한 수동 트리거 API.
 * 시연/QA 용도이며, 배치 스케줄러 자체와는 무관하게 별도로 동작한다.
 */
@Api(tags = "Transaction Sync", description = "거래내역 동기화 API")
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class DailyTransactionSyncController {

    private final TransactionSyncService transactionSyncService;
    private final MonthlyService monthlyService;

    @ApiOperation(
            value = "거래내역 동기화 + 절약 미션 지출 알림 수동 실행",
            notes = "인증 사용자 기준으로 CODEF 거래내역을 동기화하고, 오늘 신규 지출이 도전중인 절약 미션 카테고리와 겹치면 알림을 발송합니다. "
                    + "매일 00시 배치와 동일한 로직을 즉시 실행합니다."
    )
    @PostMapping("/sync-now")
    public ResponseEntity<ApiResponse<TransactionSyncResponseDTO>> syncNow(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);

        TransactionSyncResponseDTO response = transactionSyncService.syncUserTransactions(authenticatedUserId);
        monthlyService.notifyInProgressMissionExpenses(authenticatedUserId, response.getNewExpenseRecords());

        return ResponseEntity.ok(ApiResponse.success("거래내역 동기화 및 미션 알림 처리를 완료했습니다.", response));
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
