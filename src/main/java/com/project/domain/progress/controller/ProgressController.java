package com.project.domain.progress.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.progress.dto.ProgressResponseDTO;
import com.project.domain.progress.service.ProgressService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "Progress", description = "현황 API")
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @ApiOperation(value = "현황 조회", notes = "연결된 커플의 전체, 개인별, 월별 진행 현황을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<ProgressResponseDTO>> getProgress(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication
    ) {
        ProgressResponseDTO progress = progressService.getProgress(
                resolveAuthenticatedUserId(userId, authentication)
        );
        return ResponseEntity.ok(ApiResponse.success("현황 조회에 성공했습니다.", progress));
    }

    private Long resolveAuthenticatedUserId(Long userId, Authentication authentication) {
        if (userId != null) {
            return userId;
        }

        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Number) {
            return ((Number) principal).longValue();
        }
        if (principal instanceof String) {
            return Long.parseLong((String) principal);
        }
        return null;
    }
}
