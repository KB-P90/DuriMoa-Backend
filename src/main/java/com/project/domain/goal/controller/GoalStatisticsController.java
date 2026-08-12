package com.project.domain.goal.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.goal.dto.GoalAllRegionsEstimateResponseDTO;
import com.project.domain.goal.dto.GoalBudgetEstimateResponseDTO;
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

    @ApiOperation(
            value = "유형별 예상 결혼 예산 조회",
            notes = "지역별 예식장, 스튜디오, 드레스, 메이크업 통계를 합산하여 "
                    + "알뜰형, 균형형, 플렉스형 예상 금액을 조회합니다."
    )
    @GetMapping("/stat/estimate")
    public ResponseEntity<ApiResponse<GoalBudgetEstimateResponseDTO>> getGoalBudgetEstimate(
            @ApiParam(value = "지역", example = "서울(강남)", required = true)
            @RequestParam("region") String region
    ) {
        GoalBudgetEstimateResponseDTO response =
                goalStatisticsService.findGoalBudgetEstimate(region);
        return ResponseEntity.ok(
                ApiResponse.success("유형별 예상 결혼 예산 조회에 성공했습니다.", response)
        );
    }

    @ApiOperation(
            value = "주요 지역 예상 결혼 예산 조회",
            notes = "주요 지역별 예식장, 스튜디오, 드레스, 메이크업 중간값을 합산한 "
                    + "균형형 예상 결혼 예산을 조회합니다."
    )
    @GetMapping("/stat/all-regions")
    public ResponseEntity<ApiResponse<GoalAllRegionsEstimateResponseDTO>>
            getAllRegionsGoalBudgetEstimate() {
        GoalAllRegionsEstimateResponseDTO response =
                goalStatisticsService.findAllRegionsGoalBudgetEstimate();
        return ResponseEntity.ok(
                ApiResponse.success("주요 지역 예상 결혼 예산 조회에 성공했습니다.", response)
        );
    }
}
