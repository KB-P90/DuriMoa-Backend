package com.project.domain.goal.controller;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.common.response.ApiResponse;
import com.project.domain.goal.dto.GoalCreateRequestDTO;
import com.project.domain.goal.dto.GoalCreateResponseDTO;
import com.project.domain.goal.service.GoalService;
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

@Api(tags = "Goal", description = "결혼 목표 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/goal")
public class GoalController {

    private final GoalService goalService;

    @ApiOperation(value = "결혼 목표 등록", notes = "인증 사용자의 연결된 커플에 결혼 목표와 항목을 저장합니다.")
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
