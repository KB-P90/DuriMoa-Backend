package com.project.domain.goal.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.goal.dto.GoalStatisticsResponseDTO;
import com.project.domain.goal.service.GoalStatisticsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "Goal Statistics", description = "결혼 비용 통계 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/goal")
public class GoalStatisticsController {

    private final GoalStatisticsService goalStatisticsService;

    @ApiOperation(value = "결혼 통계 정보 조회", notes = "지역과 카테고리로 결혼 비용 분위값을 조회합니다.")
    @GetMapping("/stat")
    public ResponseEntity<ApiResponse<GoalStatisticsResponseDTO>> getGoalStatistics(
            @ApiParam(value = "지역", example = "서울(강남)", required = true)
            @RequestParam("region") String region,
            @ApiParam(value = "카테고리", example = "예식장", required = true)
            @RequestParam("category") String category
    ) {
        GoalStatisticsResponseDTO response =
                goalStatisticsService.findGoalStatistics(region, category);
        return ResponseEntity.ok(
                ApiResponse.success("결혼 통계 정보 조회에 성공했습니다.", response)
        );
    }
}
