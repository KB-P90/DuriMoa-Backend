package com.project.domain.goal.controller;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.common.response.ApiResponse;
import com.project.domain.goal.dto.GoalCreateRequestDTO;
import com.project.domain.goal.dto.GoalCreateResponseDTO;
import com.project.domain.goal.dto.GoalDecisionRequestDTO;
import com.project.domain.goal.dto.GoalDeleteResponseDTO;
import com.project.domain.goal.dto.GoalResponseDTO;
import com.project.domain.goal.dto.GoalStatusUpdateResponseDTO;
import com.project.domain.goal.dto.GoalUpdateRequestDTO;
import com.project.domain.goal.dto.GoalUpdateResponseDTO;
import com.project.domain.goal.service.GoalService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.List;
import javax.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "Goal", description = "결혼 시안 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/goal")
public class GoalController {

    private final GoalService goalService;

    @ApiOperation(value = "결혼 시안 등록", notes = "인증 사용자의 연결된 커플에 결혼 시안과 항목을 저장합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<GoalCreateResponseDTO>> createGoal(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication,
            @Valid @RequestBody GoalCreateRequestDTO request
    ) {

            Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
            GoalCreateResponseDTO response = goalService.createGoal(authenticatedUserId, request);
            return ResponseEntity.ok(ApiResponse.success("결혼 목표가 저장되었습니다.", response));

    }

    @ApiOperation(value = "결혼 시안 목록 조회", notes = "인증 사용자의 연결된 커플에 저장된 모든 결혼 시안을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<GoalResponseDTO>>> getGoals(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
        List<GoalResponseDTO> response = goalService.getGoals(authenticatedUserId);
        return ResponseEntity.ok(
                ApiResponse.success("결혼 목표 목록 조회에 성공했습니다.", response)
        );
    }

    @ApiOperation(value = "결혼 시안 수정", notes = "인증 사용자의 연결된 커플에 저장된 결혼 시안을 수정합니다.")
    @PutMapping("/{goalId}")
    public ResponseEntity<ApiResponse<GoalUpdateResponseDTO>> updateGoal(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication,
            @PathVariable Long goalId,
            @Valid @RequestBody GoalUpdateRequestDTO request
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
        GoalUpdateResponseDTO response =
                goalService.updateGoal(authenticatedUserId, goalId, request);
        return ResponseEntity.ok(ApiResponse.success("결혼 목표가 수정되었습니다.", response));
    }

    @ApiOperation(value = "결혼 시안 삭제", notes = "인증 사용자의 연결된 커플에 저장된 일반 결혼 시안을 삭제합니다.")
    @DeleteMapping("/{goalId}")
    public ResponseEntity<ApiResponse<GoalDeleteResponseDTO>> deleteGoal(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication,
            @PathVariable Long goalId
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
        GoalDeleteResponseDTO response = goalService.deleteGoal(authenticatedUserId, goalId);
        return ResponseEntity.ok(ApiResponse.success("결혼 목표가 삭제되었습니다.", response));
    }

    @ApiOperation(value = "메인 시안 승인 요청", notes = "일반 시안을 메인 시안 승인 대기 상태로 변경합니다.")
    @PatchMapping("/{goalId}/request")
    public ResponseEntity<ApiResponse<GoalStatusUpdateResponseDTO>> requestMainGoal(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication,
            @PathVariable Long goalId
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
        GoalStatusUpdateResponseDTO response =
                goalService.requestMainGoal(authenticatedUserId, goalId);
        return ResponseEntity.ok(
                ApiResponse.success("메인 시안 승인 요청을 전송했습니다.", response)
        );
    }

    @ApiOperation(value = "메인 시안 승인 요청 취소", notes = "본인이 전송한 메인 시안 승인 요청을 취소합니다.")
    @PatchMapping("/{goalId}/cancel")
    public ResponseEntity<ApiResponse<GoalStatusUpdateResponseDTO>> cancelMainGoalRequest(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication,
            @PathVariable Long goalId
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
        GoalStatusUpdateResponseDTO response =
                goalService.cancelMainGoalRequest(authenticatedUserId, goalId);
        return ResponseEntity.ok(
                ApiResponse.success("메인 시안 승인 요청을 취소했습니다.", response)
        );
    }

    @ApiOperation(value = "메인 시안 승인 요청 응답", notes = "상대방이 요청한 메인 시안을 승인하거나 거절합니다.")
    @PatchMapping("/{goalId}/decision")
    public ResponseEntity<ApiResponse<GoalStatusUpdateResponseDTO>> decideMainGoalRequest(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication,
            @PathVariable Long goalId,
            @Valid @RequestBody GoalDecisionRequestDTO request
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
        GoalStatusUpdateResponseDTO response =
                goalService.decideMainGoalRequest(authenticatedUserId, goalId, request);
        String message = "APPROVE".equalsIgnoreCase(request.getDecision().trim())
                ? "메인 시안 승인 요청을 수락했습니다."
                : "메인 시안 승인 요청을 거절했습니다.";
        return ResponseEntity.ok(ApiResponse.success(message, response));
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
