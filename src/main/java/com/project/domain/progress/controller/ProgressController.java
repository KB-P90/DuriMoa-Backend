package com.project.domain.progress.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.progress.dto.ProgressResponseDTO;
import com.project.domain.progress.service.ProgressService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "Progress", description = "현황 API")
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @ApiOperation(value = "현황 조회", notes = "연결된 커플의 저축 현황과 진행률을 조회합니다.")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 200, message = "현황 조회 성공"),
            @io.swagger.annotations.ApiResponse(code = 401, message = "인증된 사용자 정보가 없음"),
            @io.swagger.annotations.ApiResponse(code = 403, message = "연결된 커플이 아님"),
            @io.swagger.annotations.ApiResponse(code = 404, message = "저축 거래 내역이 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<ProgressResponseDTO>> getProgress() {
        ProgressResponseDTO progress = progressService.getProgress();
        return ResponseEntity.ok(ApiResponse.success("현황을 성공적으로 조회했습니다.", progress));
    }
}
