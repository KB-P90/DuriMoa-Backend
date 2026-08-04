package com.project.domain.goal.mapper;

import com.project.domain.goal.domain.GoalItemVO;
import com.project.domain.goal.domain.GoalVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface GoalMapper {

    Long selectCoupleIdForUpdate(@Param("coupleId") Long coupleId);

    Boolean existsGoalByCoupleId(@Param("coupleId") Long coupleId);

    Integer insertGoal(GoalVO goal);

    Integer insertGoalItems(
            @Param("goalId") Long goalId,
            @Param("items") List<GoalItemVO> items
    );

    List<GoalVO> selectGoalsByCoupleId(@Param("coupleId") Long coupleId);

    List<GoalVO> selectGoalsByCoupleIdForUpdate(@Param("coupleId") Long coupleId);

    List<GoalItemVO> selectGoalItemsByGoalIds(@Param("goalIds") List<Long> goalIds);

    Integer updateGoalRequest(
            @Param("goalId") Long goalId,
            @Param("requestedBy") Long requestedBy,
            @Param("requestedAt") LocalDateTime requestedAt
    );

    Integer cancelGoalRequest(
            @Param("goalId") Long goalId,
            @Param("requestedBy") Long requestedBy
    );

    Integer rejectGoalRequest(@Param("goalId") Long goalId);

    Integer clearMainGoal(
            @Param("coupleId") Long coupleId,
            @Param("excludedGoalId") Long excludedGoalId
    );

    Integer confirmGoal(@Param("goalId") Long goalId);
}
