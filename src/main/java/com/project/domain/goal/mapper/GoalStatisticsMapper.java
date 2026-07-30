package com.project.domain.goal.mapper;

import com.project.domain.goal.domain.GoalStatisticsVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface GoalStatisticsMapper {

    GoalStatisticsVO selectGoalStatistics(
            @Param("region") String region,
            @Param("category") String category
    );

    Integer upsertGoalStatistics(@Param("statistics") List<GoalStatisticsVO> statistics);
}
