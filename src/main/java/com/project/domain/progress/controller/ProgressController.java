package com.project.domain.progress.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.progress.dto.ProgressResponseDTO;
import com.project.domain.progress.service.ProgressService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "Progress", description = "진행 현황 API")
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @ApiOperation(value = "진행 현황 조회", notes = "연결된 커플의 전체, 개인별 예산 진행 현황을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<ProgressResponseDTO>> getProgress(
            @ApiIgnore @AuthenticationPrincipal Long userId
    ) {
        ProgressResponseDTO progress = progressService.getProgress(userId);
        return ResponseEntity.ok(ApiResponse.success("진행 현황 조회에 성공했습니다.", progress));
    }
}
