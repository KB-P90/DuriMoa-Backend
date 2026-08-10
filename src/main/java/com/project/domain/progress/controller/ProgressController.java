package com.project.domain.progress.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.progress.dto.MonthlyProgressResponseDTO;
import com.project.domain.progress.dto.ProgressItemCompletionRequestDTO;
import com.project.domain.progress.dto.ProgressItemCompletionResponseDTO;
import com.project.domain.progress.dto.ProgressResponseDTO;
import com.project.domain.progress.service.ProgressService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "Progress", description = "진행 현황 API")
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @ApiOperation(value = "전체·개인 현황 조회", notes = "연결된 커플의 전체 및 개인별 예산 진행 현황을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<ProgressResponseDTO>> getProgress(
            @ApiIgnore @AuthenticationPrincipal Long userId
    ) {
        ProgressResponseDTO progress = progressService.getProgress(userId);
        return ResponseEntity.ok(ApiResponse.success("현황 조회에 성공했습니다.", progress));
    }

    @ApiOperation(value = "월별 예산 달성 현황 조회", notes = "활성 결혼 목표의 생성월부터 현재월까지 월별 누적 지출 현황을 조회합니다.")
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<MonthlyProgressResponseDTO>> getMonthlyProgress(
            @ApiIgnore @AuthenticationPrincipal Long userId
    ) {
        MonthlyProgressResponseDTO progress = progressService.getMonthlyProgress(userId);
        return ResponseEntity.ok(ApiResponse.success("월별 예산 달성 현황 조회에 성공했습니다.", progress));
    }

    @ApiOperation(value = "카테고리 체크리스트 완료 상태 변경", notes = "활성 결혼 목표에 속한 항목의 수동 완료 상태를 변경합니다.")
    @PatchMapping("/items/{goalItemId}/completion")
    public ResponseEntity<ApiResponse<ProgressItemCompletionResponseDTO>> updateItemCompletion(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @PathVariable Long goalItemId,
            @Valid @RequestBody ProgressItemCompletionRequestDTO request
    ) {
        ProgressItemCompletionResponseDTO response = progressService.updateItemCompletion(
                userId,
                goalItemId,
                request
        );
        return ResponseEntity.ok(ApiResponse.success("체크리스트 완료 상태를 변경했습니다.", response));
    }
}
